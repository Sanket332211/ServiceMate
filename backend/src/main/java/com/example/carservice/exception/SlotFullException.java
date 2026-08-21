package com.example.carservice.exception;

/**
 * SlotFullException
 *
 * Thrown when attempting to book a time slot whose capacity (MAX 2) has already been reached.
 */
public class SlotFullException extends RuntimeException {
    public SlotFullException(String message) {
        super(message);
    }
}
