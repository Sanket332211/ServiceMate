package com.example.carservice.exception;

/**
 * DuplicateRegistrationNumberException
 *
 * Thrown when attempting to register or update a vehicle with a registration number (license plate)
 * that already exists in the system.
 */
public class DuplicateRegistrationNumberException extends RuntimeException {

    public DuplicateRegistrationNumberException(String message) {
        super(message);
    }
}
