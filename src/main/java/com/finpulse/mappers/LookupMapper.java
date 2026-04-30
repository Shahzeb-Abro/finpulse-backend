package com.finpulse.mappers;

import com.finpulse.dto.response.LookupResponse;
import com.finpulse.entity.Lookup;
import org.springframework.stereotype.Component;

@Component
public class LookupMapper {
    public LookupResponse mapLookupDomainToResponseDto(Lookup domain) {
        if (domain == null) return null;
        LookupResponse dto = new LookupResponse();
        dto.setId(domain.getId());
        dto.setLookupType(domain.getLookupType());
        dto.setLookupValue(domain.getLookupValue());
        dto.setVisibleValue(domain.getVisibleValue());
        dto.setHiddenValue(domain.getHiddenValue());
        dto.setIsUserEditable(domain.getIsUserEditable());
        return dto;
    }
}
