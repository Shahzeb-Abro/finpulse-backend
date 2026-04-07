package com.finpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BudgetThemeResponse {
    private Long id;
    private String visibleValue;
    private String lookupValue;
    private String lookupType;
    private Boolean isAlreadyUsed = Boolean.FALSE;
}
