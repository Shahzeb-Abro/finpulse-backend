package com.finpulse.service;

import com.finpulse.config.security.UserPrincipal;
import com.finpulse.entity.User;
import com.finpulse.enums.AuthProvider;
import com.finpulse.enums.Role;
import com.finpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Custom OIDC User Service — handles Google's OpenID Connect flow.
 *
 * WHY DO WE NEED THIS IN ADDITION TO CustomOAuth2UserService?
 *
 * Google supports OpenID Connect (OIDC), which is an identity layer ON TOP of OAuth2.
 * When we include "openid" in our scopes (which we do), Spring Security uses
 * the OIDC flow instead of plain OAuth2:
 *
 *   - Plain OAuth2:  DefaultOAuth2UserService → CustomOAuth2UserService
 *   - OIDC (our case): OidcUserService → THIS SERVICE (CustomOidcUserService)
 *
 * If we only register a custom OAuth2UserService (via .userService()),
 * Spring ignores it for OIDC flows and uses the default OidcUserService,
 * which returns a DefaultOidcUser — NOT our UserPrincipal.
 *
 * The fix: register THIS service via .oidcUserService() in SecurityConfig.
 *
 * The user creation/update logic is identical to CustomOAuth2UserService.
 * In a larger app, you'd extract the shared logic into a helper class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        // Let parent handle OIDC token validation + user info fetch
        OidcUser oidcUser = super.loadUser(userRequest);

        // Extract user attributes from the OIDC ID token / user info
        Map<String, Object> attributes = oidcUser.getAttributes();

        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("picture");
        Boolean emailVerified = (Boolean) attributes.get("email_verified");

        log.info("OIDC login attempt: email={}, provider=GOOGLE", email);

        // Find or create user (same logic as OAuth2 service)
        User user = userRepository.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));

        if (user != null) {
            user = updateExistingUser(user, name, avatarUrl);
            log.info("Existing user logged in via Google OIDC: {}", email);
        } else {
            user = registerNewOAuth2User(providerId, email, name, avatarUrl, emailVerified);
            log.info("New user registered via Google OIDC: {}", email);
        }

        // Return our UserPrincipal (which implements both OAuth2User and UserDetails)
        return UserPrincipal.create(user, attributes);
    }

    private User updateExistingUser(User user, String name, String avatarUrl) {
        user.setFullName(name);
        user.setAvatarUrl(avatarUrl);
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private User registerNewOAuth2User(String providerId, String email,
                                       String name, String avatarUrl, Boolean emailVerified) {
        User user = User.builder()
                .fullName(name)
                .email(email)
                .password(null)
                .avatarUrl(avatarUrl)
                .authProvider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .emailVerified(emailVerified != null && emailVerified)
                .roles(new HashSet<>(Set.of(Role.ROLE_USER)))
                .lastLoginAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }
}