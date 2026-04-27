package com.finpulse.service;

import com.finpulse.entity.PasswordResetToken;
import com.finpulse.entity.User;
import com.finpulse.exception.InvalidResetTokenException;
import com.finpulse.repository.PasswordResetTokenRepository;
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
public class PasswordResetTokenService {
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;

    @Transactional
    public String createTokenFor(User user, String ip, String userAgent) {
        tokenRepository.invalidateAllForUser(user.getId(), Instant.now());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(TOKEN_TTL));
        token.setRequestIp(ip);
        token.setRequestUserAgent(userAgent);
        tokenRepository.save(token);

        return rawToken;
    }

    @Transactional
    public User consumeToken(String rawToken) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidResetTokenException("Invalid or expired token"));

        if (token.getUsed()) {
            throw new InvalidResetTokenException("The link has already been used");
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidResetTokenException("The link has expired");
        }

        token.setUsed(true);
        token.setUsedAt(Instant.now());
        return token.getUser();
     }

     private String hash(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
     }
}
