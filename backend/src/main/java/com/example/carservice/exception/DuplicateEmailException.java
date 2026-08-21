package com.example.carservice.exception;

/**
 * DuplicateEmailException
 *
 * Thrown when a user attempts registration with an email that is already registered.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
