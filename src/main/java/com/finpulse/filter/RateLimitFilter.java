package com.finpulse.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpulse.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Filter — brute-force protection for auth endpoints.
 *
 * WHY RATE LIMITING?
 * Without it, an attacker can try thousands of password combinations
 * per second. This filter limits login/register attempts per IP.
 *
 * IMPLEMENTATION:
 * - Uses ConcurrentHashMap for thread-safe, lock-free tracking
 * - AtomicInteger for lock-free counter increments
 * - Sliding window: resets after the block duration expires
 *
 * PRODUCTION CONSIDERATIONS:
 * - In-memory storage means rate limits reset on server restart
 * - For multi-instance deployments, use Redis instead
 * - Consider using a library like Bucket4j for more sophisticated algorithms
 *
 * This filter only applies to auth-related paths (/v1/auth/*).
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.auth.requests-per-minute:10}")
    private int maxRequestsPerMinute;

    @Value("${app.rate-limit.auth.block-duration-minutes:15}")
    private int blockDurationMinutes;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Thread-safe map: IP address → request tracking info.
     * ConcurrentHashMap handles concurrent access without explicit locks.
     */
    private final ConcurrentHashMap<String, RequestTracker> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + request.getServletPath();

        RequestTracker tracker = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(blockDurationMinutes)) {
                // First request or block period expired — create fresh tracker
                return new RequestTracker();
            }
            // Increment existing counter (atomic, thread-safe)
            existing.increment();
            return existing;
        });

        if (tracker.getCount() > maxRequestsPerMinute) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, request.getServletPath());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> apiResponse = ApiResponse.error(
                    "Too many requests. Please try again in " + blockDurationMinutes + " minutes.");

            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
            return; // Block the request — don't continue filter chain
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Only apply rate limiting to authentication endpoints.
     * Other endpoints can be rate-limited separately if needed.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/v1/auth/");
    }

    /**
     * Extract real client IP, handling reverse proxies.
     *
     * When behind a load balancer/reverse proxy (Nginx, CloudFlare, AWS ALB),
     * the direct connection IP is the proxy's IP, not the client's.
     * The real client IP is in the X-Forwarded-For header.
     *
     * X-Forwarded-For format: "client, proxy1, proxy2"
     * We take the first IP (the original client).
     *
     * SECURITY NOTE: X-Forwarded-For can be spoofed. In production,
     * configure your reverse proxy to strip/overwrite this header.
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ── Inner class for tracking requests per IP ──────────────

    private static class RequestTracker {
        private final AtomicInteger count = new AtomicInteger(1);
        private final LocalDateTime windowStart = LocalDateTime.now();

        void increment() {
            count.incrementAndGet();
        }

        int getCount() {
            return count.get();
        }

        boolean isExpired(int blockMinutes) {
            return LocalDateTime.now().isAfter(windowStart.plusMinutes(blockMinutes));
        }
    }
}
