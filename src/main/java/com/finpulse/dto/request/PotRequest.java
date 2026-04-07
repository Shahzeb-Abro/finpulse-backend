package com.finpulse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PotRequest {
    @NotNull(message = "Name is required")
    @Size(min = 2, max = 30, message = "Name must be between 2 and 30 characters")
    private String name;


    private BigDecimal targetAmount;
    private Long themeId;
}
