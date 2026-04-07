package com.finpulse.dto.response;

import com.finpulse.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Response sent after successful authentication.
 *
 * Notice: NO tokens in the response body!
 * Tokens are sent as HttpOnly cookies (set in the response headers).
 * The body only contains user profile info for the frontend to display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private Long id;
    private String fullName;
    private String email;
    private String avatarUrl;
    private AuthProvider authProvider;
    private Set<String> roles;
    private String message;
}
