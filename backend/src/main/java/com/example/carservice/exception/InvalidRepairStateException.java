package com.example.carservice.exception;

/**
 * InvalidRepairStateException
 *
 * Thrown when attempting to approve or reject an additional repair that is no longer in PENDING state.
 */
public class InvalidRepairStateException extends RuntimeException {
    public InvalidRepairStateException(String message) {
        super(message);
    }
}
