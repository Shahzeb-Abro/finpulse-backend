package com.finpulse.dto.request;

import com.finpulse.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;
    private String receiverName;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
    private Date transactionDate;

    @NotNull(message = "Amount is required")
    private BigDecimal amount = BigDecimal.ZERO;
}
