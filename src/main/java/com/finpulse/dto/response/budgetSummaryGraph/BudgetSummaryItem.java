package com.finpulse.dto.response.budgetSummaryGraph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetSummaryItem {
    private Long id;
    private String name;
    private String theme;
    private BigDecimal maximumSpend = BigDecimal.ZERO;
    private BigDecimal currentSpend = BigDecimal.ZERO;
}
