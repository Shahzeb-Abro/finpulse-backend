package com.finpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecurringBillResponse {
    private Long id;
    private String title;
    private String description;
    private String categoryLookupValue;
    private Long categoryId;
    private String categoryVisibleValue;
    private String frequency;
    private LocalDate nextPaymentDate;
    private BigDecimal amount = BigDecimal.ZERO;
    private String status;
    private Boolean isPaid = Boolean.FALSE;
    private LocalDate dueDate;
}
