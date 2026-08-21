package com.example.carservice.exception;

/**
 * AiServiceUnavailableException
 *
 * Thrown when the Google Gemini AI service cannot be reached, times out,
 * encounters a rate limit, or returns an unparseable response.
 */
public class AiServiceUnavailableException extends RuntimeException {

    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
