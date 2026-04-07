package com.finpulse.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBudgetRequestDto {
    private Long budgetCategoryId;
    private Long budgetThemeId;
    private BigDecimal maximumSpend;
}
