package com.finpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetResponse {
    private Long id;
    private Long budgetCategoryId;
    private String budgetCategoryVisibleValue;
    private String budgetCategoryLookupValue;
    private Long budgetThemeId;
    private String budgetThemeVisibleValue;
    private String budgetThemeLookupValue;
    private Long budgetPeriodId;
    private String budgetPeriodVisibleValue;
    private String budgetPeriodLookupValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal maximumSpend = BigDecimal.ZERO;
    private BigDecimal currentSpend = BigDecimal.ZERO;
    private BigDecimal remainingSpend = BigDecimal.ZERO;

    List<TransactionResponse> transactions;

}
