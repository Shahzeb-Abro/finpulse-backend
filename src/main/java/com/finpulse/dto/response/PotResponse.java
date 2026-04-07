package com.finpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PotResponse {
    private Long id;
    private String name;
    private BigDecimal targetAmount = BigDecimal.ZERO;
    private BigDecimal totalSaved = BigDecimal.ZERO;
    private Long themeId;
    private String themeVisibleValue;
    private String themeLookupValue;
}
