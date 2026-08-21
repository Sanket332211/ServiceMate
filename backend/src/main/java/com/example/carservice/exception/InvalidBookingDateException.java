package com.example.carservice.exception;

/**
 * InvalidBookingDateException
 *
 * Thrown when a booking date is in the past, beyond the 7-day booking window, or has already elapsed for today.
 */
public class InvalidBookingDateException extends RuntimeException {
    public InvalidBookingDateException(String message) {
        super(message);
    }
}
