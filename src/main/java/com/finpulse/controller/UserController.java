package com.finpulse.controller;

import com.finpulse.dto.request.PreferencesRequest;
import com.finpulse.dto.response.ApiResponse;
import com.finpulse.dto.response.PreferencesResponse;
import com.finpulse.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<PreferencesResponse>> getUserPreferences() {
        return userService.getUserPreferences();
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse> updateUserPreferences(@RequestBody PreferencesRequest request) {
        return userService.updateUserPreferences(request);
    }
}
