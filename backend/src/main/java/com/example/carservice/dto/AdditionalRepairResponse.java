package com.example.carservice.dto;

import com.example.carservice.entity.AdditionalRepair;
import com.example.carservice.entity.RepairStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AdditionalRepairResponse
 *
 * DTO returned to client representing an itemized additional repair request.
 */
public class AdditionalRepairResponse {

    private Long id;
    private Long bookingId;
    private String description;
    private String reason;
    private BigDecimal estimatedAmount;
    private RepairStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime respondedAt;

    public AdditionalRepairResponse() {}

    public static AdditionalRepairResponse fromEntity(AdditionalRepair repair) {
        AdditionalRepairResponse response = new AdditionalRepairResponse();
        response.setId(repair.getId());
        if (repair.getBooking() != null) {
            response.setBookingId(repair.getBooking().getId());
        }
        response.setDescription(repair.getDescription());
        response.setReason(repair.getReason());
        response.setEstimatedAmount(repair.getEstimatedAmount());
        response.setStatus(repair.getStatus());
        response.setRequestedAt(repair.getRequestedAt());
        response.setRespondedAt(repair.getRespondedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getEstimatedAmount() {
        return estimatedAmount;
    }

    public void setEstimatedAmount(BigDecimal estimatedAmount) {
        this.estimatedAmount = estimatedAmount;
    }

    public RepairStatus getStatus() {
        return status;
    }

    public void setStatus(RepairStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
