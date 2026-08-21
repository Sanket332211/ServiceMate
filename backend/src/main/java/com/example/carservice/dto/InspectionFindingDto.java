package com.example.carservice.dto;

import com.example.carservice.entity.InspectionFinding;
import jakarta.validation.constraints.NotBlank;

/**
 * InspectionFindingDto
 *
 * DTO for component inspection observations and condition diagnostics.
 */
public class InspectionFindingDto {

    private Long id;

    @NotBlank(message = "Inspection component name cannot be blank.")
    private String component;

    @NotBlank(message = "Condition status cannot be blank.")
    private String conditionStatus;

    private String notes;

    public InspectionFindingDto() {}

    public InspectionFindingDto(String component, String conditionStatus, String notes) {
        this.component = component;
        this.conditionStatus = conditionStatus;
        this.notes = notes;
    }

    public static InspectionFindingDto fromEntity(InspectionFinding finding) {
        InspectionFindingDto dto = new InspectionFindingDto();
        dto.setId(finding.getId());
        dto.setComponent(finding.getComponent());
        dto.setConditionStatus(finding.getConditionStatus());
        dto.setNotes(finding.getNotes());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getConditionStatus() {
        return conditionStatus;
    }

    public void setConditionStatus(String conditionStatus) {
        this.conditionStatus = conditionStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
