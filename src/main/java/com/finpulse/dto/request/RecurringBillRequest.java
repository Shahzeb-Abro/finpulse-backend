package com.finpulse.dto.request;

import com.finpulse.enums.BillPeriod;
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
public class RecurringBillRequest {
    private String      title;
    private BigDecimal amount;
    private String      description;
    private BillPeriod frequency;
    private LocalDate dueDate;
    private Long       categoryId;
}
