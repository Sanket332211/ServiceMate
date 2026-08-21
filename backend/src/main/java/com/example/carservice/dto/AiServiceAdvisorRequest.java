package com.example.carservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * AiServiceAdvisorRequest
 *
 * Payload sent by customer to request an AI-assisted service recommendation.
 */
public class AiServiceAdvisorRequest {

    @NotNull(message = "Vehicle ID is required.")
    private Long vehicleId;

    @NotBlank(message = "Problem description cannot be blank.")
    @Size(min = 5, max = 1000, message = "Problem description must be between 5 and 1000 characters.")
    private String problemDescription;

    public AiServiceAdvisorRequest() {}

    public AiServiceAdvisorRequest(Long vehicleId, String problemDescription) {
        this.vehicleId = vehicleId;
        this.problemDescription = problemDescription;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }
}
