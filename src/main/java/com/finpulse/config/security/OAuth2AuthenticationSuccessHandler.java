package com.finpulse.config.security;

import com.finpulse.util.CookieUtil;
import com.finpulse.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OAuth2 Authentication Success Handler.
 *
 * This is called AFTER Google OAuth2 authentication succeeds.
 * At this point, Spring Security has:
 * 1. Received the auth code from Google
 * 2. Exchanged it for an access token
 * 3. Fetched user info from Google
 * 4. Our CustomOAuth2UserService has created/updated the user
 *
 * NOW this handler:
 * 1. Extracts the authenticated user
 * 2. Generates JWT access + refresh tokens
 * 3. Sets them as HttpOnly cookies
 * 4. Redirects to the frontend app
 *
 * WHY REDIRECT?
 * OAuth2 is a redirect-based flow. The browser went to Google's consent
 * page and came back. We can't return a JSON response (that's for AJAX).
 * Instead, we redirect to the frontend with cookies already set.
 * The frontend then picks up from the redirect URL.
 *
 * EXTENDS SimpleUrlAuthenticationSuccessHandler:
 * Provides the redirect mechanism. We override onAuthenticationSuccess()
 * to add our JWT cookie logic before the redirect.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Extract roles as string set for JWT claims
        Set<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(
                userPrincipal.getEmail(),
                userPrincipal.getId(),
                roles
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                userPrincipal.getEmail(),
                userPrincipal.getId()
        );

        // Set cookies BEFORE redirect (cookies are in response headers)
        cookieUtil.addAccessTokenCookie(response, accessToken, jwtUtil.getAccessTokenExpirationSeconds());
        cookieUtil.addRefreshTokenCookie(response, refreshToken, jwtUtil.getRefreshTokenExpirationSeconds());

        log.info("OAuth2 login successful for user: {}, redirecting to frontend", userPrincipal.getEmail());

        // Redirect to frontend's OAuth callback page
        // The frontend will read user info from a /me endpoint (using the cookie)
        String targetUrl = frontendUrl + "/oauth2/redirect";

        // Clear any authentication attributes from the session
        // (we don't need sessions — we use stateless JWT)
        clearAuthenticationAttributes(request);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
