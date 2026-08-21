package com.example.carservice.config;

import com.example.carservice.entity.Role;
import com.example.carservice.entity.User;
import com.example.carservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DataInitializer
 *
 * Seeds initial development user accounts upon application startup if they do not exist.
 * This provides a verified development SERVICE_CENTER administrative account without
 * opening public registration to administrative role escalation.
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed Development Service Center Account
            String adminEmail = "admin@servicemate.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = new User(
                        "Service Center Admin",
                        adminEmail,
                        "9876543210",
                        passwordEncoder.encode("admin123"),
                        Role.SERVICE_CENTER
                );
                userRepository.save(admin);
                logger.info("Default Service Center account created: {} (Role: SERVICE_CENTER)", adminEmail);
            }

            // Seed Sample Customer Account for immediate testing
            String customerEmail = "rahul@example.com";
            if (!userRepository.existsByEmail(customerEmail)) {
                User customer = new User(
                        "Rahul",
                        customerEmail,
                        "9876543210",
                        passwordEncoder.encode("password123"),
                        Role.CUSTOMER
                );
                userRepository.save(customer);
                logger.info("Sample customer account created: {} (Role: CUSTOMER)", customerEmail);
            }
        };
    }
}
