package com.finpulse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private Long id;
    private String description;
    private String categoryLookupValue;
    private Long categoryId;
    private String categoryVisibleValue;
    private String receiverName;
    private String transactionType;
    private LocalDate transactionDate;
    private BigDecimal amount = BigDecimal.ZERO;
}
