package com.example.carservice.controller;

import com.example.carservice.dto.*;
import com.example.carservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController
 *
 * Exposes REST endpoints for User Registration, Login, Profile inspection,
 * and temporary Role-Based Authorization verification.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH}, allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Public Registration Endpoint
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Public Login Endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Authenticated User Profile Endpoint
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserProfileResponse profile = authService.getCurrentUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    /**
     * Protected Test Endpoint for CUSTOMER Role
     * GET /api/auth/customer-test
     */
    @GetMapping("/customer-test")
    public ResponseEntity<ApiResponse> customerTest(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Access granted to CUSTOMER endpoint. Authenticated as: " + authentication.getName()
        ));
    }

    /**
     * Protected Test Endpoint for SERVICE_CENTER Role
     * GET /api/auth/service-center-test
     */
    @GetMapping("/service-center-test")
    public ResponseEntity<ApiResponse> serviceCenterTest(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Access granted to SERVICE_CENTER endpoint. Authenticated as: " + authentication.getName()
        ));
    }
}
