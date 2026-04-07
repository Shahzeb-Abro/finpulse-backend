package com.finpulse.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie Utility — manages secure cookie creation and deletion.
 *
 * WHY COOKIES INSTEAD OF BEARER TOKENS?
 *
 * Traditional approach (Bearer tokens):
 *   - Token stored in localStorage/sessionStorage
 *   - Attached manually via Authorization header
 *   - VULNERABLE to XSS attacks (any JS on the page can read localStorage)
 *
 * Our approach (HttpOnly cookies):
 *   - Token stored in browser cookies with HttpOnly flag
 *   - Automatically sent with every request (no JS needed)
 *   - HttpOnly = JavaScript CANNOT read the cookie (immune to XSS)
 *   - Secure = Cookie only sent over HTTPS
 *   - SameSite = Protection against CSRF attacks
 *
 * COOKIE SECURITY FLAGS EXPLAINED:
 *   HttpOnly: JS can't access it → prevents XSS token theft
 *   Secure:   Only sent over HTTPS → prevents network interception
 *   SameSite: Controls cross-site behavior:
 *     - "Strict": Cookie NEVER sent cross-site (breaks OAuth redirects)
 *     - "Lax":    Cookie sent on top-level navigations (ideal for OAuth)
 *     - "None":   Always sent (needed for cross-origin APIs + Secure flag)
 *   Path:     Cookie scope — "/" means all paths under the domain
 *   MaxAge:   Cookie lifetime in seconds. -1 = session cookie (deleted on browser close)
 *
 * We use ResponseCookie (Spring's builder) instead of javax Cookie because
 * ResponseCookie supports SameSite attribute which javax Cookie doesn't.
 */
@Component
@Slf4j
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "fp_access_token";
    public static final String REFRESH_TOKEN_COOKIE = "fp_refresh_token";

    @Value("${app.cookie.domain}")
    private String domain;

    @Value("${app.cookie.secure}")
    private boolean secure;

    @Value("${app.cookie.same-site}")
    private String sameSite;

    @Value("${app.cookie.path}")
    private String path;

    /**
     * Creates and attaches the access token cookie to the response.
     *
     * Access token cookie has shorter maxAge matching the token's expiry.
     * When the cookie expires, the browser automatically stops sending it,
     * triggering a 401 → frontend then uses refresh token to get a new one.
     */
    public void addAccessTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        ResponseCookie cookie = buildCookie(ACCESS_TOKEN_COOKIE, token, maxAgeSeconds);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.debug("Access token cookie set, maxAge={}s", maxAgeSeconds);
    }

    /**
     * Creates and attaches the refresh token cookie to the response.
     *
     * Refresh token cookie:
     * - Longer maxAge (7 days by default)
     * - Same security flags as access token
     * - Path could be restricted to /api/v1/auth/refresh for extra security,
     *   but "/" is simpler and the HttpOnly flag already prevents JS access
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        ResponseCookie cookie = buildCookie(REFRESH_TOKEN_COOKIE, token, maxAgeSeconds);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.debug("Refresh token cookie set, maxAge={}s", maxAgeSeconds);
    }

    /**
     * Deletes both token cookies by setting maxAge to 0.
     * Setting maxAge=0 tells the browser to immediately remove the cookie.
     * We also set the value to empty string for extra safety.
     */
    public void deleteTokenCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = buildCookie(ACCESS_TOKEN_COOKIE, "", 0);
        ResponseCookie refreshCookie = buildCookie(REFRESH_TOKEN_COOKIE, "", 0);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        log.debug("Token cookies cleared");
    }

    /**
     * Core cookie builder using Spring's ResponseCookie.
     *
     * ResponseCookie is immutable and thread-safe (builder pattern).
     * It supports SameSite which the older javax.servlet.http.Cookie doesn't.
     */
    private ResponseCookie buildCookie(String name, String value, int maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)           // JS can't read it
                .secure(secure)           // HTTPS only in production
                .sameSite(sameSite)        // CSRF protection
                .domain(domain)
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();
    }

    /**
     * Extracts a specific cookie value from the request cookies array.
     * Returns null if the cookie is not found.
     */
    public static String extractCookieValue(Cookie[] cookies, String cookieName) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
