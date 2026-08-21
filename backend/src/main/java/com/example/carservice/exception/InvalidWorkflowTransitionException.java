package com.example.carservice.exception;

/**
 * InvalidWorkflowTransitionException
 *
 * Thrown when an invalid or backward workflow state transition is attempted.
 */
public class InvalidWorkflowTransitionException extends RuntimeException {
    public InvalidWorkflowTransitionException(String message) {
        super(message);
    }
}
