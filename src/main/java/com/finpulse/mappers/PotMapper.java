package com.finpulse.mappers;

import com.finpulse.dto.response.PotResponse;
import com.finpulse.entity.Pot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PotMapper {

    public PotResponse mapPotDomainToResponseDto(Pot domain) {
        if (domain == null) return null;
        PotResponse dto = new PotResponse();
        dto.setId(domain.getId());
        dto.setName(domain.getName());
        dto.setTargetAmount(domain.getTargetAmount());
        dto.setTotalSaved(domain.getTotalSaved());
        dto.setThemeId(domain.getPotTheme().getId());
        dto.setThemeVisibleValue(domain.getPotTheme().getVisibleValue());
        dto.setThemeLookupValue(domain.getPotTheme().getLookupValue());

        return dto;
    }
}
