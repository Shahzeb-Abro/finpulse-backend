package com.finpulse.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class AuditorConfig {
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("SYSTEM");
            }

            return Optional.of(authentication.getName());
        };
    }
}