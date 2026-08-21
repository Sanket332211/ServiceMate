package com.example.carservice.exception;

/**
 * ResourceNotFoundException
 *
 * Thrown when a requested entity (such as a Vehicle) cannot be found by its identifier.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
