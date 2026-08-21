package com.example.carservice.exception;

/**
 * DuplicateBookingException
 *
 * Thrown when attempting to book the same vehicle on the same date and time slot.
 */
public class DuplicateBookingException extends RuntimeException {
    public DuplicateBookingException(String message) {
        super(message);
    }
}
