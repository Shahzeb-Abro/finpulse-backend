package com.finpulse.entity;

import com.finpulse.enums.AuthProvider;
import com.finpulse.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Core User entity mapped to the "users" table.
 *
 * Design decisions:
 * - Uses UUID-style generation but Long id for performance (index-friendly)
 * - Stores auth provider to distinguish local vs OAuth users
 * - Password is nullable because Google OAuth users don't have one
 * - Roles stored as ElementCollection for simple RBAC without a join entity
 * - Refresh token stored in DB for server-side revocation capability
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_provider_id", columnList = "providerId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Nullable because OAuth2 users authenticate via provider, not password.
     * For local users, this stores the BCrypt-hashed password.
     */
    @Column(length = 255)
    private String password;

    @Column(nullable = false)
    private Instant passwordChangedAt;

    @Column(length = 512)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider authProvider;

    /**
     * The unique ID from the OAuth2 provider (e.g., Google's "sub" claim).
     * Null for local users.
     */
    @Column(length = 255)
    private String providerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    /**
     * Stores the current refresh token hash.
     * When a user logs out or rotates tokens, this is cleared/updated,
     * instantly invalidating any old refresh tokens.
     */
    @Column(length = 512)
    private String refreshTokenHash;

    @Column
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ── Helper Methods ───────────────────────────────────────

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }
}
