package com.finpulse.controller;

import com.finpulse.config.security.UserPrincipal;
import com.finpulse.dto.request.ForgotPasswordRequest;
import com.finpulse.dto.request.LoginRequest;
import com.finpulse.dto.request.RegisterRequest;
import com.finpulse.dto.request.ResetPasswordRequest;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.AuthResponse;
import com.finpulse.exception.AuthenticationException;
import com.finpulse.service.AuthService;
import com.finpulse.service.PasswordResetService;
import com.finpulse.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller — the PUBLIC FACE of our auth system.
 *
 * This controller exposes REST endpoints for:
 *
 * POST /v1/auth/register  — Create new account (local)
 * POST /v1/auth/login     — Login with email/password
 * POST /v1/auth/refresh   — Get new access token using refresh token
 * POST /v1/auth/logout    — Logout (clear tokens)
 * GET  /v1/auth/me        — Get current user's profile
 *
 * DESIGN DECISIONS:
 *
 * 1. All auth endpoints use POST (except /me) because they modify state
 *    (creating tokens, clearing tokens, etc.)
 *
 * 2. @Valid on request bodies triggers Bean Validation (defined in DTOs).
 *    Invalid requests get a 400 response with field-level error messages.
 *
 * 3. @AuthenticationPrincipal injects the current authenticated user
 *    directly into the method parameter — no need to manually extract
 *    from SecurityContext.
 *
 * 4. HttpServletResponse is injected to set cookies (response headers).
 *
 * 5. ResponseEntity wraps responses with proper HTTP status codes.
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /**
     * POST /v1/auth/register
     *
     * Registers a new user with email + password.
     * On success, tokens are set as cookies and user info is returned.
     *
     * Request body:
     * {
     *   "fullName": "John Doe",
     *   "email": "john@example.com",
     *   "password": "MySecureP@ss123"
     * }
     *
     * Response (201 Created):
     * {
     *   "success": true,
     *   "message": "Registration successful",
     *   "data": { "id": 1, "fullName": "John Doe", ... }
     * }
     *
     * + Set-Cookie: fp_access_token=xxx; HttpOnly; Secure; SameSite=Lax
     * + Set-Cookie: fp_refresh_token=xxx; HttpOnly; Secure; SameSite=Lax
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.register(request, response);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", authResponse));
    }

    /**
     * POST /v1/auth/login
     *
     * Authenticates with email + password.
     * Sets JWT cookies on success.
     *
     * Request body:
     * {
     *   "email": "john@example.com",
     *   "password": "MySecureP@ss123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request, response);

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * POST /v1/auth/refresh
     *
     * Exchanges a valid refresh token (from cookie) for new tokens.
     * Implements refresh token rotation — old token is invalidated.
     *
     * No request body needed — the refresh token is in the cookie.
     *
     * Flow:
     * 1. Extract refresh token from cookie
     * 2. Validate it (signature + expiry + DB hash)
     * 3. Generate new access + refresh tokens
     * 4. Set new cookies, invalidate old refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = CookieUtil.extractCookieValue(
                request.getCookies(),
                CookieUtil.REFRESH_TOKEN_COOKIE
        );

        if (refreshToken == null) {
            throw new AuthenticationException("No refresh token found");
        }

        AuthResponse authResponse = authService.refreshToken(refreshToken, response);

        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
    }

    /**
     * POST /v1/auth/logout
     *
     * Logs out the current user:
     * 1. Clears refresh token hash from database
     * 2. Deletes both token cookies
     *
     * @AuthenticationPrincipal automatically extracts the user from
     * the SecurityContext (which was set by JwtAuthenticationFilter).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletResponse response) {

        authService.logout(userPrincipal.getId(), response);

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    /**
     * GET /v1/auth/me
     *
     * Returns the current authenticated user's profile.
     * Used by the frontend to:
     * 1. Verify the user is still authenticated
     * 2. Display user info (name, avatar, etc.)
     * 3. Check roles for conditional UI rendering
     *
     * This is the endpoint the React app calls after OAuth2 redirect
     * to get the user's info (since the redirect only sets cookies,
     * it doesn't return user data).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        AuthResponse authResponse = AuthResponse.builder()
                .id(userPrincipal.getId())
                .fullName(userPrincipal.getFullName())
                .email(userPrincipal.getEmail())
                .avatarUrl(userPrincipal.getAvatarUrl())
                .roles(userPrincipal.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(java.util.stream.Collectors.toSet()))
                .message("Authenticated")
                .build();

        return ResponseEntity.ok(ApiResponse.success("User retrieved", authResponse));
    }

    /**
     * POST /v1/auth/forgot-password
     *
     * Initiates a password reset flow.
     *
     * SECURITY NOTE: Always returns the same response whether the email
     * exists or not. This prevents email enumeration attacks where an
     * attacker probes the system to discover which emails are registered.
     *
     * If the email exists:
     *   1. Invalidate any prior unused reset tokens for this user
     *   2. Generate a cryptographically random token (256 bits)
     *   3. Store its SHA-256 hash with a 30-minute expiry
     *   4. Email the raw token as a link to the user
     *
     * If the email doesn't exist: silently do nothing.
     *
     * Request body:
     * {
     *   "email": "john@example.com"
     * }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        passwordResetService.requestReset(request, httpRequest);

        return ResponseEntity.ok(ApiResponse.success(
                "If an account exists for that email, a reset link has been sent."
        ));
    }

    /**
     * POST /v1/auth/reset-password
     *
     * Completes a password reset using a token from the email link.
     *
     * Flow:
     *   1. Validate token (exists, not expired, not used)
     *   2. Hash and update the user's password
     *   3. Mark token as used (single-use)
     *   4. Invalidate all existing refresh tokens (force re-login on all devices)
     *   5. Send confirmation email — critical for catching account takeover
     *
     * Request body:
     * {
     *   "token": "abc123...",
     *   "newPassword": "MyNewSecureP@ss123"
     * }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(request);

        return ResponseEntity.ok(ApiResponse.success(
                "Password updated successfully. Please log in with your new password."
        ));
    }
}
