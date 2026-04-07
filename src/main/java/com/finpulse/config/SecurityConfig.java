package com.finpulse.config;

import com.finpulse.config.security.CookieOAuth2AuthorizationRequestRepository;
import com.finpulse.config.security.JwtAuthenticationEntryPoint;
import com.finpulse.config.security.OAuth2AuthenticationFailureHandler;
import com.finpulse.config.security.OAuth2AuthenticationSuccessHandler;
import com.finpulse.filter.JwtAuthenticationFilter;
import com.finpulse.filter.RateLimitFilter;
import com.finpulse.service.CustomOAuth2UserService;
import com.finpulse.service.CustomOidcUserService;
import com.finpulse.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * MASTER SECURITY CONFIGURATION
 *
 * This is the most critical file in the entire application.
 * It configures HOW Spring Security protects our app.
 *
 * KEY CONCEPTS:
 *
 * 1. FILTER CHAIN:
 *    Spring Security is built on a chain of filters. Each request passes
 *    through these filters in order. We customize which filters run and
 *    where our custom JWT filter sits in the chain.
 *
 *    Request → [RateLimit] → [JWT] → [UsernamePassword] → [OAuth2] → Controller
 *
 * 2. STATELESS SESSIONS:
 *    We disable HTTP sessions entirely. Every request must carry its own
 *    authentication (via JWT cookie). This makes our app:
 *    - Horizontally scalable (no session replication needed)
 *    - More secure (no session hijacking possible)
 *    - REST-compliant (stateless by design)
 *
 * 3. CSRF DISABLED:
 *    Traditional CSRF protection uses session-based tokens.
 *    Since we're stateless + using SameSite cookies, we don't need CSRF.
 *    SameSite=Lax prevents cross-site request forgery at the browser level.
 *
 * 4. BCrypt PASSWORD ENCODING:
 *    BCrypt is the industry standard for password hashing:
 *    - Automatically salts passwords (each hash is unique)
 *    - Configurable work factor (strength=12 means 2^12 rounds)
 *    - Intentionally slow to prevent brute-force attacks
 *    - A single BCrypt hash takes ~250ms vs ~1μs for MD5
 *
 * @EnableWebSecurity — Activates Spring Security's web security support
 * @EnableMethodSecurity — Enables @PreAuthorize, @Secured annotations on methods
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService oAuth2UserService;
    private final CustomOidcUserService oidcUserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;
    private final CookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    /**
     * BCrypt password encoder with strength 12.
     *
     * Strength = work factor = log2(iterations).
     * 12 means 2^12 = 4,096 iterations.
     * Each password hash takes ~250ms — fast enough for login,
     * slow enough to make brute-force impractical.
     *
     * @Bean makes this available for dependency injection anywhere.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication provider that connects:
     * - UserDetailsService (loads user from DB)
     * - PasswordEncoder (verifies password hash)
     *
     * When AuthenticationManager.authenticate() is called,
     * it delegates to this provider which:
     * 1. Calls userDetailsService.loadUserByUsername(email)
     * 2. Compares submitted password with stored BCrypt hash
     * 3. Returns Authentication object if valid
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Hide whether the failure was due to bad username or bad password
        // (prevents username enumeration attacks)
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    /**
     * Exposes AuthenticationManager as a bean.
     * We inject this into AuthService to perform programmatic authentication
     * during the login flow (instead of relying on form-based auth).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * THE MAIN SECURITY FILTER CHAIN.
     *
     * This is where everything comes together.
     * Read this method top-to-bottom — it tells the story of how
     * a request is processed through our security system.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── 1. DISABLE DEFAULTS WE DON'T NEED ───────────────
                .csrf(AbstractHttpConfigurer::disable)         // SameSite cookies handle CSRF
                .formLogin(AbstractHttpConfigurer::disable)    // No server-side login form
                .httpBasic(AbstractHttpConfigurer::disable)    // No Basic auth

                // ── 2. STATELESS SESSION MANAGEMENT ─────────────────
                // NEVER create HTTP sessions. Every request must authenticate via JWT.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── 3. CUSTOM ENTRY POINT ───────────────────────────
                // When an unauthenticated user hits a protected endpoint,
                // return 401 JSON instead of redirecting to a login page.
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // ── 4. URL-BASED AUTHORIZATION RULES ────────────────
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no authentication required
                        .requestMatchers(
                                "/v1/auth/**",           // login, register, refresh, logout
                                "/oauth2/**",             // Google OAuth2 flow
                                "/actuator/health"        // health check for load balancers
                        ).permitAll()

                        // Allow preflight CORS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Admin-only endpoints
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // ── 5. GOOGLE OAUTH2 CONFIGURATION ──────────────────
                .oauth2Login(oauth2 -> oauth2
                        // Store OAuth2 authorization request in cookie (not session)
                        // This is what makes OAuth2 work with STATELESS sessions.
                        .authorizationEndpoint(auth -> auth
                                .baseUri("/oauth2/authorize")
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                        // The callback URI base — Spring appends /{registrationId} automatically.
                        // loginProcessingUrl is the actual URL the filter matches against.
                        .loginProcessingUrl("/oauth2/callback/*")
                        // Custom services that process Google user data
                        // userService → plain OAuth2 flows
                        // oidcUserService → OpenID Connect flows (Google with "openid" scope)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oAuth2UserService)
                                .oidcUserService(oidcUserService))
                        // After successful Google auth → generate JWTs + redirect
                        .successHandler(oAuth2SuccessHandler)
                        // After failed Google auth → redirect with error
                        .failureHandler(oAuth2FailureHandler)
                )

                // ── 6. AUTHENTICATION PROVIDER ──────────────────────
                .authenticationProvider(authenticationProvider())

                // ── 7. CUSTOM FILTER PLACEMENT ──────────────────────
                // Our JWT filter runs BEFORE Spring's default UsernamePasswordFilter.
                // This means JWT auth is checked first for every request.
                // Rate limit filter runs before JWT filter for early rejection.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
