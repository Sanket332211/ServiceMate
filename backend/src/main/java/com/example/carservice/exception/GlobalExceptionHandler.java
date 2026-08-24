package com.example.carservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler
 *
 * Catches exceptions thrown across all REST controllers and maps them into
 * clean, predictable JSON error responses.
 *
 * Explicitly attaches full CORS response headers (Access-Control-Allow-Origin,
 * Access-Control-Allow-Credentials, etc.) to every error response to ensure
 * browsers never drop or mask backend errors with generic CORS preflight/fetch failures.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private HttpHeaders createCorsHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (request != null) {
            String origin = request.getHeader("Origin");
            if (origin != null && !origin.isBlank()) {
                headers.set("Access-Control-Allow-Origin", origin);
                headers.set("Access-Control-Allow-Credentials", "true");
            } else {
                headers.set("Access-Control-Allow-Origin", "*");
            }
        } else {
            headers.set("Access-Control-Allow-Origin", "*");
        }
        headers.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT, PATCH");
        headers.set("Access-Control-Max-Age", "3600");
        headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-Requested-With, Origin, Access-Control-Request-Method, Access-Control-Request-Headers, remember-me");
        headers.set("Access-Control-Expose-Headers", "Authorization, Link, X-Total-Count");
        headers.set("Vary", "Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
        return headers;
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(DuplicateRegistrationNumberException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateRegistrationNumber(DuplicateRegistrationNumberException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(SlotFullException.class)
    public ResponseEntity<Map<String, Object>> handleSlotFull(SlotFullException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.CONFLICT.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(DuplicateBookingException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateBooking(DuplicateBookingException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.CONFLICT.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(InvalidBookingDateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBookingDate(InvalidBookingDateException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(InvalidWorkflowTransitionException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidWorkflowTransition(InvalidWorkflowTransitionException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(InvalidRepairStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRepairState(InvalidRepairStateException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.UNAUTHORIZED.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Invalid email or password.");
        error.put("status", HttpStatus.UNAUTHORIZED.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Validation failed. Please check the input fields.");
        error.put("errors", fieldErrors);
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Malformed JSON request body or unreadable parameters.");
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Content type is not supported. Please use application/json.");
        error.put("status", HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "HTTP method not supported: " + ex.getMethod());
        error.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Access denied: You do not have permission to access this resource.");
        error.put("status", HttpStatus.FORBIDDEN.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "Unauthorized: Authentication is required to access this resource.");
        error.put("status", HttpStatus.UNAUTHORIZED.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleAiServiceUnavailable(AiServiceUnavailableException ex, HttpServletRequest request) {
        log.warn("AI service unavailable: {}", ex.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", ex.getMessage());
        error.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).headers(createCorsHeaders(request)).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception caught by GlobalExceptionHandler: {}", ex.getMessage(), ex);
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", "An unexpected error occurred. Please try again later.");
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).headers(createCorsHeaders(request)).body(error);
    }
}
