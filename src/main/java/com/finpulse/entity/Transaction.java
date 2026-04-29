package com.finpulse.entity;

import com.finpulse.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TRANSACTIONS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "active_flag = true")
public class Transaction extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "transaction_category_id")
    private Lookup category;

    private BigDecimal amount = BigDecimal.ZERO;
    private String description;
    private Boolean activeFlag;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    private LocalDate transactionDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
