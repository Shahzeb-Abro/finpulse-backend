package com.finpulse.repository;

import com.finpulse.entity.User;
import com.finpulse.enums.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA repository for User entity.
 *
 * Spring Data auto-implements these methods by parsing the method names.
 * Custom @Query annotations are used for operations that need
 * more control or performance optimization.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Find user by email (used for login + duplicate check) */
    Optional<User> findByEmail(String email);

    /** Check if email is already registered */
    boolean existsByEmail(String email);

    /** Find by OAuth provider + provider's user ID (for OAuth login linking) */
    Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);

    /**
     * Update only the refresh token hash without loading the full entity.
     * This is a performance optimization — we don't need to fetch the user
     * just to update one field.
     */
    @Modifying
    @Query("UPDATE User u SET u.refreshTokenHash = :tokenHash WHERE u.id = :userId")
    void updateRefreshTokenHash(@Param("userId") Long userId, @Param("tokenHash") String tokenHash);

    /**
     * Update last login timestamp. Separated from main update
     * to avoid unnecessary dirty-checking overhead.
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") Long userId, @Param("loginTime") LocalDateTime loginTime);

    /**
     * Clear refresh token on logout — instantly invalidates the token
     * even if it hasn't expired yet.
     */
    @Modifying
    @Query("UPDATE User u SET u.refreshTokenHash = null WHERE u.id = :userId")
    void clearRefreshToken(@Param("userId") Long userId);

    Optional<User> findByEmailIgnoreCase(String email);
}
