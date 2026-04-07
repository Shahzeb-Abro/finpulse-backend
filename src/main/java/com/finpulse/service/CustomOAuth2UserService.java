package com.finpulse.service;

import com.finpulse.config.security.UserPrincipal;
import com.finpulse.entity.User;
import com.finpulse.enums.AuthProvider;
import com.finpulse.enums.Role;
import com.finpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Custom OAuth2 User Service — handles the Google OAuth2 login flow.
 *
 * GOOGLE OAUTH2 FLOW (high level):
 *
 * 1. User clicks "Login with Google" on frontend
 * 2. Frontend redirects to: /oauth2/authorize/google
 * 3. Spring redirects to Google's consent screen
 * 4. User grants permission → Google redirects back with auth code
 * 5. Spring exchanges auth code for access token (server-side, secure)
 * 6. Spring calls Google's user info endpoint with the access token
 * 7. THIS SERVICE receives the user info and:
 *    a. If user exists → update their info (name, avatar might change)
 *    b. If new user → create account automatically
 * 8. Returns UserPrincipal for Spring Security to use
 *
 * EXTENDS DefaultOAuth2UserService:
 * The default service handles steps 5-6 (token exchange + user info fetch).
 * We override loadUser() to add our custom logic (step 7).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // Let the parent class do the heavy lifting:
        // exchange code → get access token → fetch user info from Google
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract Google's user info attributes
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId = (String) attributes.get("sub");       // Google's unique user ID
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("picture");
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        log.info("OAuth2 login attempt: email={}, provider=GOOGLE", email);

        // Look up user by Google provider ID first (most reliable)
        // Fall back to email lookup (handles account linking)
        User user = userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));

        if (user != null) {
            // Existing user — update their Google profile info
            // (name, avatar, etc. can change on Google's side)
            user = updateExistingUser(user, name, avatarUrl);
            log.info("Existing user logged in via Google: {}", email);
        } else {
            // New user — create account automatically
            user = registerNewOAuth2User(providerId, email, name, avatarUrl, emailVerified);
            log.info("New user registered via Google: {}", email);
        }

        return UserPrincipal.create(user, attributes);
    }

    /**
     * Updates an existing user's profile with latest Google data.
     * Google users might change their name or profile picture.
     */
    private User updateExistingUser(User user, String name, String avatarUrl) {
        user.setFullName(name);
        user.setAvatarUrl(avatarUrl);
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Creates a new user from Google OAuth2 data.
     *
     * Note: password is null — Google users don't have a local password.
     * They authenticate through Google every time.
     * emailVerified comes from Google — if Google says it's verified, we trust it.
     */
    private User registerNewOAuth2User(String providerId, String email,
                                        String name, String avatarUrl, Boolean emailVerified) {
        User user = User.builder()
                .fullName(name)
                .email(email)
                .password(null)                    // No password for OAuth users
                .avatarUrl(avatarUrl)
                .authProvider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .emailVerified(emailVerified != null && emailVerified)
                .roles(Set.of(Role.ROLE_USER))
                .lastLoginAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }
}
