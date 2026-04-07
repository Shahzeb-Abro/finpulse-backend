package com.finpulse.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpulse.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom Authentication Entry Point.
 *
 * By default, Spring Security redirects unauthenticated requests to a login page.
 * That's fine for server-rendered apps, but for our REST API + SPA frontend,
 * we need to return a JSON 401 response instead.
 *
 * This component is invoked whenever an unauthenticated user tries to access
 * a protected endpoint. It sends back a clean JSON error response.
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt: {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> apiResponse = ApiResponse.error(
                "Authentication required. Please log in to access this resource.");

        objectMapper.findAndRegisterModules(); // for LocalDateTime serialization
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
