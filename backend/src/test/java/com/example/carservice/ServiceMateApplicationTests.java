package com.example.carservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ServiceMateApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring Boot application context bootstraps successfully
    }
}
