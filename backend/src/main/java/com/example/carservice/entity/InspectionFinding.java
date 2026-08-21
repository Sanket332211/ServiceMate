package com.example.carservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * InspectionFinding
 *
 * Represents an individual mechanical, electrical, or structural component inspection observation.
 */
@Entity
@Table(name = "inspection_findings")
public class InspectionFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_record_id", nullable = false)
    @JsonIgnore
    private ServiceRecord serviceRecord;

    @Column(nullable = false)
    private String component; // e.g. "Brakes", "Battery", "Tyres", "Engine Oil", "AC"

    @Column(nullable = false)
    private String conditionStatus; // e.g. "Good", "Fair", "Needs Attention", "Replaced"

    @Column(columnDefinition = "TEXT")
    private String notes;

    public InspectionFinding() {}

    public InspectionFinding(ServiceRecord serviceRecord, String component, String conditionStatus, String notes) {
        this.serviceRecord = serviceRecord;
        this.component = component;
        this.conditionStatus = conditionStatus;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceRecord getServiceRecord() {
        return serviceRecord;
    }

    public void setServiceRecord(ServiceRecord serviceRecord) {
        this.serviceRecord = serviceRecord;
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
