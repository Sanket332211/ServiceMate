package com.example.carservice.service;

import com.example.carservice.dto.AuthResponse;
import com.example.carservice.dto.LoginRequest;
import com.example.carservice.dto.RegisterRequest;
import com.example.carservice.dto.UserProfileResponse;
import com.example.carservice.entity.Role;
import com.example.carservice.entity.User;
import com.example.carservice.exception.DuplicateEmailException;
import com.example.carservice.exception.InvalidCredentialsException;
import com.example.carservice.repository.UserRepository;
import com.example.carservice.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService
 *
 * Implements business logic for User registration, credential verification,
 * password hashing (BCrypt), and JWT token issuance.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new customer account.
     * Public registration ALWAYS assigns the CUSTOMER role.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 1. Check for existing email
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new DuplicateEmailException("An account with this email already exists.");
        }

        // 2. Hash password securely using BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Instantiate and persist User entity with CUSTOMER role
        User user = new User(
                request.getName().trim(),
                request.getEmail().trim().toLowerCase(),
                request.getPhone() != null ? request.getPhone().trim() : null,
                hashedPassword,
                Role.CUSTOMER
        );

        User savedUser = userRepository.save(user);

        // 4. Generate signed JWT token
        String token = jwtService.generateToken(savedUser);

        // 5. Return AuthResponse
        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                "Registration successful."
        );
    }

    /**
     * Authenticates existing user credentials and returns a JWT token.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null || request.getEmail().isBlank()) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        String email = request.getEmail().trim().toLowerCase();

        // 1. Look up user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password."));

        // 2. Verify password with BCrypt hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        // 3. Generate signed JWT token
        String token = jwtService.generateToken(user);

        // 4. Return AuthResponse
        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                "Login successful."
        );
    }

    /**
     * Fetches profile details for the authenticated user.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidCredentialsException("User not found.");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("User not found."));
        return new UserProfileResponse(user);
    }
}
