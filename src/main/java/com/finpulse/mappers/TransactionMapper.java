package com.finpulse.mappers;

import com.finpulse.dto.response.TransactionResponse;
import com.finpulse.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionMapper {

    public TransactionResponse mapDomainToResponseDto(Transaction domain) {
        if (domain == null) return null;

        TransactionResponse dto = new TransactionResponse();
        dto.setId(domain.getId());
        dto.setCategoryId(domain.getCategory().getId());
        dto.setCategoryVisibleValue(domain.getCategory().getVisibleValue());
        dto.setCategoryLookupValue(domain.getCategory().getLookupValue());
        dto.setDescription(domain.getDescription());
        dto.setAmount(domain.getAmount());
        dto.setTransactionType(domain.getTransactionType().name());
        dto.setTransactionDate(domain.getTransactionDate());

        return dto;
    }
}
