package com.finpulse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "EMAIL_TEMPLATES")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class EmailTemplate extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code; // WELCOME, BUDGET_ALERT etc

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    private String description;

    @Column(nullable = false)
    private Boolean activeFlag = Boolean.TRUE;
}
