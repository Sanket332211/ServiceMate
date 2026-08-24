package com.example.carservice.config;

import com.example.carservice.security.CustomUserDetailsService;
import com.example.carservice.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig (Phase 2 Full Security Implementation)
 *
 * Configures stateless Spring Security with BCrypt hashing, JWT authentication filter,
 * public vs protected route authorization, and role-based access control for
 * CUSTOMER and SERVICE_CENTER roles.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService,
                          org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .authorizeHttpRequests(auth -> auth
                // Preflight OPTIONS requests must always be permitted without authentication
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(org.springframework.web.cors.CorsUtils::isPreFlightRequest).permitAll()

                // Role-specific verification test endpoints (must precede general /api/auth/** wildcard)
                .requestMatchers("/api/auth/customer-test").hasRole("CUSTOMER")
                .requestMatchers("/api/auth/service-center-test").hasRole("SERVICE_CENTER")

                // Public authentication and utility endpoints accessible without authentication
                .requestMatchers("/api/auth/**", "/ws-servicemate/**", "/error").permitAll()

                // Role-based route foundations for future modules
                .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                .requestMatchers("/api/service-center/**").hasRole("SERVICE_CENTER")

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Custom Exception Handlers for 401 Unauthorized and 403 Forbidden with CORS injection
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    setCorsHeaders(request, response);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"success\":false,\"message\":\"Unauthorized: Authentication is required to access this resource.\",\"status\":401}");
                    response.getWriter().flush();
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    setCorsHeaders(request, response);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"success\":false,\"message\":\"Access denied: You do not have permission to access this resource.\",\"status\":403}");
                    response.getWriter().flush();
                })
            )
            // Register JWT filter before standard username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void setCorsHeaders(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT, PATCH");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-Requested-With, Origin, Access-Control-Request-Method, Access-Control-Request-Headers, remember-me");
        response.setHeader("Access-Control-Expose-Headers", "Authorization, Link, X-Total-Count");
        response.setHeader("Vary", "Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
    }
}
