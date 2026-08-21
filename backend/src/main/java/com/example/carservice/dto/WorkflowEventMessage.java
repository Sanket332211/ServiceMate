package com.example.carservice.dto;

import com.example.carservice.entity.WorkflowStatus;
import java.time.LocalDateTime;

/**
 * WorkflowEventMessage
 *
 * Payload broadcast over STOMP WebSocket topics on service workflow or repair events.
 */
public class WorkflowEventMessage {

    private String eventType;
    private Long bookingId;
    private WorkflowStatus workflowStatus;
    private String message;
    private LocalDateTime timestamp;

    public WorkflowEventMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public WorkflowEventMessage(String eventType, Long bookingId, WorkflowStatus workflowStatus, String message) {
        this.eventType = eventType;
        this.bookingId = bookingId;
        this.workflowStatus = workflowStatus;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public WorkflowStatus getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(WorkflowStatus workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
