package com.finpulse.service;

import com.finpulse.config.security.UserPrincipal;
import com.finpulse.entity.User;
import com.finpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation.
 *
 * Spring Security's authentication mechanism calls this service
 * to load user details during the login process. Here's the flow:
 *
 * 1. User submits email + password
 * 2. Spring Security's AuthenticationManager receives the credentials
 * 3. It calls THIS service's loadUserByUsername() to get the user from DB
 * 4. Spring Security compares the submitted password with the stored hash
 * 5. If they match → authentication successful
 * 6. If not → AuthenticationException thrown
 *
 * We also have loadUserById() for the JWT filter — when a request
 * comes in with a valid JWT, we need to load the full user to set
 * up the SecurityContext.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Called by Spring Security during local authentication.
     * "Username" in our case is the email address.
     *
     * @Transactional(readOnly = true) — optimization hint to Hibernate:
     * no dirty checking needed, can use read-only database connection.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login attempt with non-existent email: {}", email);
                    // Generic message to prevent email enumeration attacks
                    return new UsernameNotFoundException("Invalid email or password");
                });

        return UserPrincipal.create(user);
    }

    /**
     * Called by JwtAuthenticationFilter to load user from JWT's userId claim.
     * This is faster than loadUserByUsername because ID lookups use the
     * primary key index (O(1) vs potential full scan).
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("JWT contained non-existent userId: {}", userId);
                    return new UsernameNotFoundException("User not found with id: " + userId);
                });

        return UserPrincipal.create(user);
    }
}
