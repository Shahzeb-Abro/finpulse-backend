package com.finpulse.service;

import com.finpulse.dto.request.PreferencesRequest;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.PreferencesResponse;
import com.finpulse.entity.User;
import com.finpulse.mappers.PreferencesMapper;
import com.finpulse.repository.UserRepository;
import com.finpulse.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final SecurityUtils securityUtils;
    private final PreferencesMapper preferencesMapper;
    private final UserRepository userRepository;

    public ResponseEntity<ApiResponse<PreferencesResponse>> getUserPreferences() {
        User loggedInUser = securityUtils.getCurrentUser();

        return ResponseEntity.ok(ApiResponse.success("Preferences fetched successfully", preferencesMapper.mapPreferencesDomainToResponseDto(loggedInUser)));
    }

    public ResponseEntity<ApiResponse> updateUserPreferences(PreferencesRequest preferences) {
        User loggedInUser = securityUtils.getCurrentUser();
        if (preferences.currency() != null) {
            loggedInUser.setCurrency(preferences.currency());
            userRepository.save(loggedInUser);
        }

        return ResponseEntity.ok(ApiResponse.success("Preferences updated successfully"));
    }
}
