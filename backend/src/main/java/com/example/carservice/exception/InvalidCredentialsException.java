package com.example.carservice.exception;

/**
 * InvalidCredentialsException
 *
 * Thrown when login fails due to non-existent email or mismatched password.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
