package com.finpulse.mappers;

import com.finpulse.dto.response.RecurringBillResponse;
import com.finpulse.entity.RecurringBill;
import org.springframework.stereotype.Component;

@Component
public class RecurringBillMapper {
    public RecurringBillResponse mapDomainToResponseDto(RecurringBill domain) {
        if (domain == null) return null;

        RecurringBillResponse dto = new RecurringBillResponse();
        dto.setId(domain.getId());
        dto.setTitle(domain.getTitle());
        dto.setDescription(domain.getDescription());
        dto.setAmount(domain.getAmount());
        dto.setFrequency(domain.getPeriod().name());
        dto.setStatus(domain.getStatus().name());
        dto.setIsPaid(domain.getStatus().name().equals("PAID"));

        dto.setCategoryId(domain.getCategory().getId());
        dto.setCategoryVisibleValue(domain.getCategory().getVisibleValue());
        dto.setCategoryLookupValue(domain.getCategory().getLookupValue());
        dto.setNextPaymentDate(domain.getNextPaymentDate());
        dto.setDueDate(domain.getDueDate());
        return dto;
    }
}
