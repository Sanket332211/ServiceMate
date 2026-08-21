package com.example.carservice.entity;

/**
 * WorkflowStatus
 *
 * Represents the physical workshop service stages of a vehicle in ServiceMate Phase 5.
 * Note: CONFIRMED belongs to ServiceBooking.status; ServiceWorkflow is initialized
 * upon vehicle intake with CAR_RECEIVED.
 */
public enum WorkflowStatus {
    CAR_RECEIVED("Car Received at Workshop"),
    INSPECTION("Initial 40-Point Inspection"),
    SERVICE_IN_PROGRESS("Service & Repair in Progress"),
    AWAITING_APPROVAL("Awaiting Customer Approval for Additional Repair"),
    QUALITY_CHECK("Final Quality & Road-Test Check"),
    READY_FOR_DELIVERY("Ready for Customer Handover"),
    COMPLETED("Service Completed & Vehicle Delivered");

    private final String displayName;

    WorkflowStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
