package com.finpulse.repository;

import com.finpulse.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
           UPDATE EmailVerificationToken t
              SET t.used = true
            WHERE t.user.id = :userId AND t.used = false
           """)
    void invalidateAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken t WHERE t.expiresAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}