package com.finpulse.dto.response.budgetSummaryGraph;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BudgetSummaryGraph {
    private BigDecimal totalMaximumSpend = BigDecimal.ZERO;
    private BigDecimal totalCurrentSpend = BigDecimal.ZERO;

    List<BudgetSummaryItem> budgetSummaryItems;
}
