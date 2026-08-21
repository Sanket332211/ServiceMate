package com.example.carservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ServiceMateApplication
 *
 * The main entry point for the ServiceMate backend Spring Boot application.
 * This class initializes Spring context, auto-configuration, and embedded Tomcat.
 */
@SpringBootApplication
public class ServiceMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceMateApplication.class, args);
    }
}
