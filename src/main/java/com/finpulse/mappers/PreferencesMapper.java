package com.finpulse.mappers;

import com.finpulse.dto.response.PreferencesResponse;
import com.finpulse.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PreferencesMapper {

    public PreferencesResponse mapPreferencesDomainToResponseDto(User domain) {
        if (domain == null) return null;

        PreferencesResponse dto = new PreferencesResponse();
        dto.setCurrency(domain.getCurrency());
        return dto;
    }

}
