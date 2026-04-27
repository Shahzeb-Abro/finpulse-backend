package com.finpulse.service;

import com.finpulse.dto.request.ForgotPasswordRequest;
import com.finpulse.dto.request.ResetPasswordRequest;
import com.finpulse.entity.User;
import com.finpulse.repository.UserRepository;
import com.finpulse.util.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void requestReset(ForgotPasswordRequest forgotPasswordRequest, HttpServletRequest request) {
        // Always behave the same whether user exists or not — no enumeration leak
        userRepository.findByEmailIgnoreCase(forgotPasswordRequest.email()).ifPresent(user -> {
            try {
                String rawToken = tokenService.createTokenFor(user, request.getRemoteAddr(), request.getHeader("User-Agent"));
                String resetLink = frontendUrl + "/reset-password?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);

                emailService.sendTemplatedEmail(
                        user.getEmail(),
                        "PASSWORD_RESET",
                        Map.of(
                                "name", user.getFullName(),
                                "resetLink", resetLink,
                                "expiresInMinutes", 30
                        )
                );

            } catch (Exception e) {
                log.error("Error sending password reset email", e);
            }
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = tokenService.consumeToken(request.token());

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        emailService.sendTemplatedEmail(
                user.getEmail(),
                "PASSWORD_CHANGED_NOTIFICATION",
                Map.of("name", user.getFullName(), "changedAt", Instant.now())
        );
    }
}
