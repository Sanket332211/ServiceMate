package com.example.carservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CorsConfig
 *
 * Configures Cross-Origin Resource Sharing (CORS) to allow the Angular frontend
 * (local development or production deployed URL) to communicate with this Spring Boot backend.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:${FRONTEND_URL:http://localhost:4200}}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed Origins & Origin Patterns (supports dynamic env var, Vercel wildcard, and local development)
        List<String> originPatterns = new ArrayList<>(Arrays.asList(
                "https://*.vercel.app",
                "https://service-mate-one.vercel.app",
                "http://localhost:4200",
                "http://localhost:3000",
                "http://127.0.0.1:4200"
        ));

        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !originPatterns.contains(s))
                    .forEach(originPatterns::add);
        }
        
        configuration.setAllowedOriginPatterns(originPatterns);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Link", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
