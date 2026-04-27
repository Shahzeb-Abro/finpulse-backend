package com.finpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "email_verification_tokens",
        indexes = {
                @Index(name = "idx_evt_token_hash", columnList = "tokenHash"),
                @Index(name = "idx_evt_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class EmailVerificationToken extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;
}