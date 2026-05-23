package com.finpulse.entity;

import com.finpulse.enums.BillPeriod;
import com.finpulse.enums.BillStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "RECURRING_BILLS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "active_flag = true")
public class RecurringBill extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private BigDecimal amount;
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Lookup category;

    @Enumerated(EnumType.STRING)
    private BillStatus status;

    @Enumerated(EnumType.STRING)
    private BillPeriod period;

    private Long linkedTransactionId;

    private Boolean activeFlag = true;

    private LocalDate dueDate; // Day for Monthly and Month + Day for Yearly frequency
    private LocalDate nextPaymentDate;

}

