package com.example.carservice.entity;

/**
 * BookingStatus
 *
 * Represents the lifecycle states of a ServiceMate service booking.
 * In Phase 4, newly created bookings become CONFIRMED, and users may CANCEL them.
 * Note: Only CONFIRMED bookings consume time slot capacity.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
