package com.example.carservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ServiceWorkflow
 *
 * Tracks the physical workshop execution milestones and timestamps for a vehicle service.
 * Maps to the `service_workflows` table.
 */
@Entity
@Table(name = "service_workflows")
public class ServiceWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private ServiceBooking booking;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private WorkflowStatus status;

    @Column(name = "car_received_at")
    private LocalDateTime carReceivedAt;

    @Column(name = "inspection_started_at")
    private LocalDateTime inspectionStartedAt;

    @Column(name = "service_started_at")
    private LocalDateTime serviceStartedAt;

    @Column(name = "quality_check_started_at")
    private LocalDateTime qualityCheckStartedAt;

    @Column(name = "ready_for_delivery_at")
    private LocalDateTime readyForDeliveryAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ServiceWorkflow() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ServiceWorkflow(ServiceBooking booking, WorkflowStatus status) {
        this.booking = booking;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (status == WorkflowStatus.CAR_RECEIVED) {
            this.carReceivedAt = LocalDateTime.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceBooking getBooking() {
        return booking;
    }

    public void setBooking(ServiceBooking booking) {
        this.booking = booking;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public LocalDateTime getCarReceivedAt() {
        return carReceivedAt;
    }

    public void setCarReceivedAt(LocalDateTime carReceivedAt) {
        this.carReceivedAt = carReceivedAt;
    }

    public LocalDateTime getInspectionStartedAt() {
        return inspectionStartedAt;
    }

    public void setInspectionStartedAt(LocalDateTime inspectionStartedAt) {
        this.inspectionStartedAt = inspectionStartedAt;
    }

    public LocalDateTime getServiceStartedAt() {
        return serviceStartedAt;
    }

    public void setServiceStartedAt(LocalDateTime serviceStartedAt) {
        this.serviceStartedAt = serviceStartedAt;
    }

    public LocalDateTime getQualityCheckStartedAt() {
        return qualityCheckStartedAt;
    }

    public void setQualityCheckStartedAt(LocalDateTime qualityCheckStartedAt) {
        this.qualityCheckStartedAt = qualityCheckStartedAt;
    }

    public LocalDateTime getReadyForDeliveryAt() {
        return readyForDeliveryAt;
    }

    public void setReadyForDeliveryAt(LocalDateTime readyForDeliveryAt) {
        this.readyForDeliveryAt = readyForDeliveryAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
