package com.finpulse.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBudgetRequestDto {
    private Long budgetCategoryId;
    private Long budgetThemeId;
    private Long budgetPeriodId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal maximumSpend;
}
