package com.finpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "PASSWORD_RESET_TOKENS", indexes = {
        @Index(name = "idx_prt_token_hash", columnList = "token_hash"),
        @Index(name = "idx_prt_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Boolean used = Boolean.FALSE;

    private Instant usedAt;

    private String requestIp;
    private String requestUserAgent;
}
