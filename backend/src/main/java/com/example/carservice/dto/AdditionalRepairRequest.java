package com.example.carservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * AdditionalRepairRequest
 *
 * DTO for Service Center submitting an unexpected repair discovery with an itemized estimate.
 */
public class AdditionalRepairRequest {

    @NotBlank(message = "Repair description cannot be blank.")
    private String description;

    @NotBlank(message = "Reason for repair cannot be blank.")
    private String reason;

    @NotNull(message = "Estimated amount is required.")
    @DecimalMin(value = "1.00", message = "Estimated amount must be greater than 0.")
    private BigDecimal estimatedAmount;

    public AdditionalRepairRequest() {}

    public AdditionalRepairRequest(String description, String reason, BigDecimal estimatedAmount) {
        this.description = description;
        this.reason = reason;
        this.estimatedAmount = estimatedAmount;
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
}
