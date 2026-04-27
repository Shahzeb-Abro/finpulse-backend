package com.finpulse.service;

import com.finpulse.entity.EmailVerificationToken;
import com.finpulse.entity.User;
import com.finpulse.exception.InvalidVerificationTokenException;
import com.finpulse.repository.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationTokenService {

    private static final Duration TOKEN_TTL = Duration.ofHours(24);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;

    @Transactional
    public String createTokenFor(User user) {
        // Invalidate any existing unused tokens
        tokenRepository.invalidateAllForUser(user.getId());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        tokenRepository.save(token);

        log.info("Email verification token created for userId={}", user.getId());
        return rawToken;
    }

    @Transactional
    public User consumeToken(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidVerificationTokenException("Invalid or expired verification link"));

        if (token.isUsed()) {
            throw new InvalidVerificationTokenException("This verification link has already been used");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidVerificationTokenException("This verification link has expired");
        }

        token.setUsed(true);
        return token.getUser();
    }

    private String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}