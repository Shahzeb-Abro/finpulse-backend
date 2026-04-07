package com.finpulse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "BUDGETS")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Budget extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "budget_category_id")
    private Lookup budgetCategory;

    @ManyToOne
    @JoinColumn(name = "budget_theme_id")
    private Lookup budgetTheme;

    private BigDecimal maximumSpend = BigDecimal.ZERO;
    private BigDecimal currentSpend = BigDecimal.ZERO;
    private BigDecimal remainingSpend = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Boolean activeFlag;
}
