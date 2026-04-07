package com.finpulse.config.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

/**
 * Cookie-Based OAuth2 Authorization Request Repository.
 *
 * THE PROBLEM:
 * OAuth2 is a multi-step redirect flow:
 *   Step 1: Our server creates an OAuth2AuthorizationRequest (with state, nonce)
 *           and redirects the user to Google.
 *   Step 2: Google redirects back with an auth code + state parameter.
 *   Step 3: Our server needs the ORIGINAL AuthorizationRequest from Step 1
 *           to verify the state parameter matches (CSRF protection).
 *
 * By default, Spring stores this in the HTTP session. But we use
 * SessionCreationPolicy.STATELESS — no sessions exist!
 *
 * THE SOLUTION:
 * Store the OAuth2AuthorizationRequest in a short-lived cookie.
 * The cookie travels with the browser through the redirect flow:
 *   Step 1: Server serializes AuthorizationRequest → Base64 → cookie
 *   Step 2: Browser sends cookie back with Google's callback
 *   Step 3: Server deserializes cookie → verifies state → deletes cookie
 *
 * This keeps us 100% stateless while supporting OAuth2.
 */
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_AUTH_REQUEST_COOKIE = "fp_oauth2_auth_request";
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3 minutes — plenty for OAuth redirect

    /**
     * Called during Step 2 (callback) to retrieve the original authorization request.
     * Deserializes it from the cookie.
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, OAUTH2_AUTH_REQUEST_COOKIE);
    }

    /**
     * Called during Step 1 to save the authorization request before redirecting to Google.
     * If authorizationRequest is null, it means we should remove it (cleanup).
     */
    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (authorizationRequest == null) {
            deleteCookie(request, response, OAUTH2_AUTH_REQUEST_COOKIE);
            return;
        }

        // Serialize the authorization request to Base64 and store in cookie
        String serialized = Base64.getUrlEncoder().encodeToString(
                SerializationUtils.serialize(authorizationRequest));

        Cookie cookie = new Cookie(OAUTH2_AUTH_REQUEST_COOKIE, serialized);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(COOKIE_EXPIRE_SECONDS);
        response.addCookie(cookie);
    }

    /**
     * Called after the OAuth2 flow completes (success or failure).
     * Removes the cookie and returns the authorization request for final verification.
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {

        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        if (authRequest != null) {
            deleteCookie(request, response, OAUTH2_AUTH_REQUEST_COOKIE);
        }
        return authRequest;
    }

    // ── Helper Methods ────────────────────────────────────────

    /**
     * Deserializes the OAuth2AuthorizationRequest from a cookie.
     */
    @SuppressWarnings("deprecation")
    private OAuth2AuthorizationRequest getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                try {
                    byte[] decoded = Base64.getUrlDecoder().decode(cookie.getValue());
                    return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(decoded);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Deletes a cookie by setting maxAge to 0.
     */
    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return;

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                Cookie cleared = new Cookie(name, "");
                cleared.setPath("/");
                cleared.setHttpOnly(true);
                cleared.setMaxAge(0);
                response.addCookie(cleared);
                break;
            }
        }
    }
}