package com.example.carservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * ServiceCompletionRequest
 *
 * Payload submitted by the Service Center during the QUALITY_CHECK stage to record
 * service mileage, completion summary, itemized work performed, and inspection findings.
 */
public class ServiceCompletionRequest {

    @NotNull(message = "Service mileage is required.")
    @Min(value = 0, message = "Mileage cannot be negative.")
    private Integer mileage;

    @NotBlank(message = "Service summary cannot be blank.")
    private String serviceSummary;

    @NotEmpty(message = "At least one service work item must be provided.")
    @Valid
    private List<ServiceItemDto> items = new ArrayList<>();

    @NotEmpty(message = "At least one inspection finding must be provided.")
    @Valid
    private List<InspectionFindingDto> inspectionFindings = new ArrayList<>();

    public ServiceCompletionRequest() {}

    public ServiceCompletionRequest(Integer mileage, String serviceSummary,
                                    List<ServiceItemDto> items,
                                    List<InspectionFindingDto> inspectionFindings) {
        this.mileage = mileage;
        this.serviceSummary = serviceSummary;
        this.items = items;
        this.inspectionFindings = inspectionFindings;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }

    public String getServiceSummary() {
        return serviceSummary;
    }

    public void setServiceSummary(String serviceSummary) {
        this.serviceSummary = serviceSummary;
    }

    public List<ServiceItemDto> getItems() {
        return items;
    }

    public void setItems(List<ServiceItemDto> items) {
        this.items = items;
    }

    public List<InspectionFindingDto> getInspectionFindings() {
        return inspectionFindings;
    }

    public void setInspectionFindings(List<InspectionFindingDto> inspectionFindings) {
        this.inspectionFindings = inspectionFindings;
    }
}
