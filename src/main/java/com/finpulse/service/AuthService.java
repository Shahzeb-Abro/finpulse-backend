package com.finpulse.service;

import com.finpulse.config.security.UserPrincipal;
import com.finpulse.dto.request.LoginRequest;
import com.finpulse.dto.request.RegisterRequest;
import com.finpulse.dto.response.AuthResponse;
import com.finpulse.entity.User;
import com.finpulse.enums.AuthProvider;
import com.finpulse.enums.Role;
import com.finpulse.exception.AuthenticationException;
import com.finpulse.exception.ResourceAlreadyExistsException;
import com.finpulse.repository.UserRepository;
import com.finpulse.util.CookieUtil;
import com.finpulse.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication Service — the BRAIN of our auth system.
 *
 * This service orchestrates all authentication operations:
 * 1. REGISTER: Create new local accounts
 * 2. LOGIN:    Authenticate with email/password
 * 3. REFRESH:  Exchange a refresh token for new access token
 * 4. LOGOUT:   Invalidate tokens (clear cookies + DB)
 *
 * SECURITY PRINCIPLES APPLIED:
 *
 * 1. PASSWORD NEVER STORED IN PLAIN TEXT
 *    BCrypt hashes the password before storing. Even if the DB is compromised,
 *    passwords can't be reversed.
 *
 * 2. REFRESH TOKEN ROTATION
 *    Each refresh generates a NEW refresh token and invalidates the old one.
 *    If a token is stolen, the attacker gets one use before it's rotated.
 *
 * 3. REFRESH TOKEN HASH IN DB
 *    We store a SHA-256 hash of the refresh token, not the token itself.
 *    Even if the DB is breached, the attacker can't use the hashes.
 *
 * 4. SERVER-SIDE TOKEN INVALIDATION
 *    On logout, we clear the refresh token hash from the DB.
 *    Even if the JWT hasn't expired, the refresh token can't be used.
 *
 * 5. GENERIC ERROR MESSAGES
 *    Login failures always say "Invalid credentials" — never revealing
 *    whether the email or password was wrong (prevents enumeration).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;

    // ═══════════════════════════════════════════════════════════
    //  REGISTER
    // ═══════════════════════════════════════════════════════════

    /**
     * Registers a new local user.
     *
     * Flow:
     * 1. Check if email already exists
     * 2. Hash the password with BCrypt
     * 3. Create user with ROLE_USER
     * 4. Generate JWT tokens
     * 5. Set cookies and return user info
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {

        // Check for existing email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "An account with this email already exists");
        }

        // Create the user entity
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .roles(new HashSet<>(Set.of(Role.ROLE_USER)))
                .lastLoginAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Generate tokens and set cookies
        return authenticateAndRespond(user, response, "Registration successful");
    }

    // ═══════════════════════════════════════════════════════════
    //  LOGIN
    // ═══════════════════════════════════════════════════════════

    /**
     * Authenticates a user with email + password.
     *
     * Flow:
     * 1. AuthenticationManager delegates to DaoAuthenticationProvider
     * 2. DaoAuthenticationProvider calls CustomUserDetailsService.loadUserByUsername()
     * 3. It loads the user from DB and compares BCrypt password hash
     * 4. If valid → returns Authentication object with UserPrincipal
     * 5. If invalid → throws AuthenticationException
     * 6. We generate JWTs and set cookies
     *
     * The try-catch wraps Spring's AuthenticationException (from spring-security)
     * and converts it to our custom AuthenticationException for consistent error handling.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        try {
            // This single line triggers the entire authentication chain:
            // AuthManager → DaoProvider → UserDetailsService → BCrypt compare
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new AuthenticationException("User not found"));

            // Update last login timestamp
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("User logged in: {}", user.getEmail());

            return authenticateAndRespond(user, response, "Login successful");

        } catch (org.springframework.security.core.AuthenticationException ex) {
            // Generic message — don't reveal if email or password was wrong
            log.warn("Login failed for email: {}", request.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  REFRESH TOKEN
    // ═══════════════════════════════════════════════════════════

    /**
     * Exchanges a valid refresh token for new access + refresh tokens.
     *
     * REFRESH TOKEN ROTATION:
     * Every refresh request generates a NEW refresh token and invalidates
     * the old one. This limits the damage from token theft:
     *
     * - Attacker steals refresh token
     * - Attacker uses it → gets new tokens, old one invalidated
     * - Real user tries to use old token → FAILS
     * - User is forced to re-login → attacker's tokens become useless
     *
     * WHY VERIFY HASH IN DB?
     * Even though the JWT signature proves the token is authentic,
     * we also check the DB hash to ensure the token hasn't been revoked
     * (e.g., after logout or a previous rotation).
     */
    @Transactional
    public AuthResponse refreshToken(String refreshToken, HttpServletResponse response) {

        // Step 1: Validate JWT structure + signature + expiry
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid or expired refresh token");
        }

        // Step 2: Verify it's actually a refresh token (not an access token)
        String tokenType = jwtUtil.extractTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new AuthenticationException("Invalid token type");
        }

        // Step 3: Extract user ID and load user
        Long userId = jwtUtil.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        // Step 4: Verify token hash matches what's stored in DB
        // This catches revoked tokens (after logout or previous rotation)
        String tokenHash = hashToken(refreshToken);
        if (user.getRefreshTokenHash() == null || !user.getRefreshTokenHash().equals(tokenHash)) {
            log.warn("Refresh token hash mismatch for user: {} — possible token reuse attack", user.getEmail());
            // Clear ALL tokens — force re-login (security measure)
            userRepository.clearRefreshToken(userId);
            throw new AuthenticationException("Refresh token has been revoked");
        }

        log.info("Token refreshed for user: {}", user.getEmail());

        // Step 5: Generate new tokens (rotation)
        return authenticateAndRespond(user, response, "Token refreshed successfully");
    }

    // ═══════════════════════════════════════════════════════════
    //  LOGOUT
    // ═══════════════════════════════════════════════════════════

    /**
     * Logs out the user by:
     * 1. Clearing refresh token hash from DB (server-side invalidation)
     * 2. Deleting token cookies (client-side cleanup)
     *
     * Even if an old access token is still valid (not expired),
     * it will expire within 15 minutes. The refresh token is
     * immediately invalidated, so no new access tokens can be obtained.
     */
    @Transactional
    public void logout(Long userId, HttpServletResponse response) {
        userRepository.clearRefreshToken(userId);
        cookieUtil.deleteTokenCookies(response);
        log.info("User logged out: userId={}", userId);
    }

    // ═══════════════════════════════════════════════════════════
    //  HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Common logic for all auth flows: generate tokens + set cookies + build response.
     *
     * Used by register, login, and refresh to avoid code duplication.
     */
    private AuthResponse authenticateAndRespond(User user, HttpServletResponse response, String message) {

        Set<String> roles = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toSet());

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getId(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail(), user.getId());

        // Store refresh token hash in DB for server-side validation
        String refreshTokenHash = hashToken(refreshToken);
        user.setRefreshTokenHash(refreshTokenHash);
        userRepository.save(user);

        // Set HttpOnly cookies
        cookieUtil.addAccessTokenCookie(response, accessToken, jwtUtil.getAccessTokenExpirationSeconds());
        cookieUtil.addRefreshTokenCookie(response, refreshToken, jwtUtil.getRefreshTokenExpirationSeconds());

        // Return user info (NOT tokens — those are in the cookies)
        return AuthResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .authProvider(user.getAuthProvider())
                .roles(roles)
                .message(message)
                .build();
    }

    /**
     * Hashes a token using SHA-256.
     *
     * WHY HASH THE REFRESH TOKEN?
     * If an attacker gets read access to the database, they'd see
     * refresh tokens in plain text and could impersonate any user.
     * By storing only the hash, a DB breach doesn't compromise tokens.
     *
     * SHA-256 is perfect here (vs BCrypt) because:
     * - Refresh tokens are high-entropy (random-looking JWTs)
     * - No risk of dictionary/rainbow table attacks
     * - SHA-256 is fast — we need to hash on every request
     * - BCrypt's slowness would add unnecessary latency
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
