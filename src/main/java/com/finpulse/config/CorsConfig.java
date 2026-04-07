package com.finpulse.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) Configuration.
 *
 * WHY IS CORS NEEDED?
 * Browsers enforce the Same-Origin Policy — JavaScript from origin A
 * (e.g., localhost:5173) cannot make requests to origin B
 * (e.g., localhost:8080) unless origin B explicitly allows it.
 *
 * Since our React frontend runs on a different port than our backend,
 * we MUST configure CORS to allow cross-origin requests.
 *
 * KEY SETTINGS:
 * - allowedOrigins: Which frontend URLs can access our API
 * - allowedMethods: Which HTTP methods are permitted
 * - allowCredentials(true): CRITICAL for cookies! Without this,
 *   the browser won't send cookies with cross-origin requests.
 * - allowedHeaders: Which request headers are allowed
 * - exposedHeaders: Which response headers the frontend can read
 * - maxAge: How long the browser caches preflight responses
 *
 * SECURITY: In production, allowedOrigins should be your exact frontend
 * domain(s), never "*" when using credentials.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Set-Cookie")
                .allowCredentials(true)   // MUST be true for cookies to work cross-origin
                .maxAge(3600);            // Cache preflight for 1 hour
    }

    /**
     * Also register as a CorsConfigurationSource bean for Spring Security.
     * Spring Security has its own CORS handling that runs before Spring MVC's.
     * Without this bean, Spring Security might reject CORS requests
     * before they reach our MVC CORS config.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
