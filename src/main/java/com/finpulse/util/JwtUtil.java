package com.finpulse.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * JWT Utility — the brain of our token-based auth system.
 *
 * Architecture decisions:
 *
 * 1. TWO TOKEN STRATEGY (Access + Refresh):
 *    - Access Token:  Short-lived (15 min). Sent with every request.
 *                     If stolen, damage is limited to 15 minutes.
 *    - Refresh Token: Long-lived (7 days). Used ONLY to get new access tokens.
 *                     Stored as HttpOnly cookie — JS can't touch it.
 *
 * 2. HS512 SIGNING:
 *    - HMAC-SHA512 — symmetric key algorithm.
 *    - Same key signs and verifies (vs RSA where sign/verify use different keys).
 *    - Faster than RSA. Perfect for monolithic apps where the same server
 *      both creates and validates tokens.
 *
 * 3. CLAIMS:
 *    - "sub" (subject): User's email — the primary identifier.
 *    - "userId": Database ID for quick lookups.
 *    - "roles": User's roles for authorization decisions.
 *    - "type": Distinguishes access vs refresh tokens.
 *
 * 4. WHY NOT RSA?
 *    - RSA is needed in microservices where Service A creates tokens
 *      and Services B, C, D verify them (using the public key).
 *    - For our monolith, HS512 is simpler and 10x faster.
 */
@Component
@Slf4j
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    /**
     * Constructor injection with @Value.
     * The secret is read from application.properties / environment variables.
     * Keys.hmacShaKeyFor() creates a proper SecretKey from the raw bytes.
     */
    public JwtUtil(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {

        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    // ═══════════════════════════════════════════════════════════
    //  TOKEN GENERATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Generates a short-lived access token.
     * Contains user identity + roles for authorization.
     */
    public String generateAccessToken(String email, Long userId, Set<String> roles) {
        return buildToken(email, userId, roles, "access", accessTokenExpirationMs);
    }

    /**
     * Generates a long-lived refresh token.
     * Contains minimal claims — only used to issue new access tokens.
     */
    public String generateRefreshToken(String email, Long userId) {
        return buildToken(email, userId, Set.of(), "refresh", refreshTokenExpirationMs);
    }

    /**
     * Core token builder. All tokens go through here.
     *
     * Token structure (decoded):
     * {
     *   "header": { "alg": "HS512", "typ": "JWT" },
     *   "payload": {
     *     "sub": "user@email.com",
     *     "userId": 1,
     *     "roles": ["ROLE_USER"],
     *     "type": "access",
     *     "iat": 1700000000,
     *     "exp": 1700000900
     *   },
     *   "signature": "..."
     * }
     */
    private String buildToken(String email, Long userId, Set<String> roles,
                              String tokenType, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)                          // "sub" claim
                .claims(Map.of(                          // custom claims
                        "userId", userId,
                        "roles", roles,
                        "type", tokenType
                ))
                .issuedAt(now)                           // "iat" claim
                .expiration(expiry)                      // "exp" claim
                .signWith(signingKey)                    // sign with HS512
                .compact();                              // serialize to string
    }

    // ═══════════════════════════════════════════════════════════
    //  TOKEN VALIDATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates a token's signature and expiration.
     * Returns true only if the token is structurally valid,
     * properly signed, and not expired.
     *
     * Each exception type represents a different failure mode:
     * - ExpiredJwt: Token was valid but is now past its expiry
     * - SignatureException: Token was tampered with
     * - MalformedJwt: Token string is not valid JWT format
     * - UnsupportedJwt: Token uses an algorithm we don't support
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired: {}", ex.getMessage());
        } catch (SignatureException ex) {
            log.error("JWT signature validation failed — possible tampering: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Malformed JWT: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  CLAIM EXTRACTION
    // ═══════════════════════════════════════════════════════════

    public String extractEmail(String token) {
        return parseToken(token).getPayload().getSubject();
    }

    public Long extractUserId(String token) {
        return parseToken(token).getPayload().get("userId", Long.class);
    }

    public String extractTokenType(String token) {
        return parseToken(token).getPayload().get("type", String.class);
    }

    /**
     * Core parser — verifies signature + parses claims in one step.
     * If the signature doesn't match, this throws immediately
     * (before you can read any claims).
     */
    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }

    // ═══════════════════════════════════════════════════════════
    //  EXPIRATION INFO (for cookie max-age)
    // ═══════════════════════════════════════════════════════════

    /** Returns access token TTL in seconds (for cookie maxAge) */
    public int getAccessTokenExpirationSeconds() {
        return (int) (accessTokenExpirationMs / 1000);
    }

    /** Returns refresh token TTL in seconds (for cookie maxAge) */
    public int getRefreshTokenExpirationSeconds() {
        return (int) (refreshTokenExpirationMs / 1000);
    }
}
