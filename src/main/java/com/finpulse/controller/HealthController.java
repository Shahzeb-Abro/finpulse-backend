package com.finpulse.controller;

import com.finpulse.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check endpoint.
 * Used by load balancers, monitoring tools, and quick API verification.
 */
@RestController
@RequestMapping("/v1")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(
                ApiResponse.success("FinPulse API is running", Map.of(
                        "status", "UP",
                        "version", "1.0.0"
                ))
        );
    }
}
