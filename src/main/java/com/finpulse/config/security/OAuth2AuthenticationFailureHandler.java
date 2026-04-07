package com.finpulse.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 Authentication Failure Handler.
 *
 * Called when Google OAuth2 authentication fails. This can happen if:
 * - User denies permission on Google's consent screen
 * - Google's servers are unreachable
 * - The OAuth configuration is wrong (bad client ID/secret)
 * - Token exchange fails
 *
 * We redirect to the frontend's login page with an error query parameter
 * so the frontend can display an appropriate error message.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        // URL-encode the error message to safely pass it as a query parameter
        String errorMessage = URLEncoder.encode(
                exception.getLocalizedMessage(), StandardCharsets.UTF_8);

        String targetUrl = frontendUrl + "/login?error=" + errorMessage;

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
