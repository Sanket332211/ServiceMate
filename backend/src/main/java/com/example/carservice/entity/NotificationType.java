package com.example.carservice.entity;

/**
 * NotificationType
 *
 * Classifies automated in-app notifications generated during the service lifecycle.
 */
public enum NotificationType {
    SERVICE_STATUS_UPDATED,
    REPAIR_REQUESTED,
    REPAIR_APPROVED,
    REPAIR_REJECTED,
    VEHICLE_READY,
    SERVICE_COMPLETED,
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED
}
