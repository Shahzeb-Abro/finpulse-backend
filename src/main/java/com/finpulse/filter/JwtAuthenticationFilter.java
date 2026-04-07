package com.finpulse.filter;

import com.finpulse.service.CustomUserDetailsService;
import com.finpulse.util.CookieUtil;
import com.finpulse.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — the GATEKEEPER of our application.
 *
 * This filter runs ONCE per request (OncePerRequestFilter guarantees this).
 * It sits in Spring Security's filter chain and does the following:
 *
 * REQUEST FLOW:
 * ┌─────────┐    ┌──────────────┐    ┌─────────────┐    ┌────────────┐
 * │ Browser │───▶│ JWT Filter   │───▶│ Security    │───▶│ Controller │
 * │ Request │    │ (this class) │    │ Filters     │    │            │
 * └─────────┘    └──────────────┘    └─────────────┘    └────────────┘
 *                      │
 *               1. Extract JWT from cookie
 *               2. Validate JWT signature + expiry
 *               3. Load user from DB
 *               4. Set SecurityContext
 *               5. Pass to next filter
 *
 * WHY OncePerRequestFilter?
 * In Spring, a request might pass through the filter chain multiple times
 * (e.g., after a forward or include). OncePerRequestFilter ensures our
 * JWT logic runs exactly once, preventing duplicate DB lookups.
 *
 * IMPORTANT: This filter only handles ACCESS tokens.
 * Refresh tokens are handled by the AuthController's /refresh endpoint.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Step 1: Extract JWT from the access_token cookie
            String jwt = extractJwtFromCookie(request);

            // Step 2: If token exists and is valid, authenticate the request
            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {

                // Step 3: Verify this is an access token (not a refresh token)
                String tokenType = jwtUtil.extractTokenType(jwt);
                if (!"access".equals(tokenType)) {
                    log.warn("Non-access token used for API request");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Step 4: Extract user ID from token and load full user details
                Long userId = jwtUtil.extractUserId(jwt);
                UserDetails userDetails = userDetailsService.loadUserById(userId);

                // Step 5: Create Spring Security authentication token
                // This is NOT the JWT — it's Spring's internal auth representation
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,           // principal (the user)
                                null,                  // credentials (null — already verified via JWT)
                                userDetails.getAuthorities()  // roles/permissions
                        );

                // Attach request details (IP, session ID, etc.) for audit logging
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                // Step 6: Set the SecurityContext — this is what makes
                // @PreAuthorize, @Secured, and SecurityContextHolder.getContext()
                // work throughout the request lifecycle.
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authenticated user: {}, URI: {}", userId, request.getRequestURI());
            }
        } catch (Exception ex) {
            // CRITICAL: Never let filter exceptions crash the request.
            // Log the error and continue — the request will be treated as
            // unauthenticated, and Spring Security will return 401 if the
            // endpoint requires authentication.
            log.error("Could not set user authentication in security context", ex);
        }

        // Always continue the filter chain — even if auth failed.
        // The security configuration decides what happens to unauthenticated requests.
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT from the access_token cookie.
     *
     * Cookie-based extraction (our approach):
     *   - Browser automatically sends cookies with every request
     *   - No frontend code needed to attach tokens
     *   - HttpOnly cookies can't be read by JavaScript
     *
     * vs. Bearer token extraction (traditional approach):
     *   - Frontend must manually add "Authorization: Bearer <token>" header
     *   - Token typically stored in localStorage (vulnerable to XSS)
     */
    private String extractJwtFromCookie(HttpServletRequest request) {
        return CookieUtil.extractCookieValue(
                request.getCookies(),
                CookieUtil.ACCESS_TOKEN_COOKIE
        );
    }

    /**
     * Skip JWT filter for public endpoints.
     * These paths don't require authentication, so no point
     * in attempting JWT extraction and validation.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/v1/auth/login")
                || path.startsWith("/v1/auth/register")
                || path.startsWith("/oauth2/")
                || path.startsWith("/actuator/health");
    }
}
