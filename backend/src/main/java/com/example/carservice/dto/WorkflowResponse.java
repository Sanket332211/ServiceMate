package com.example.carservice.dto;

import com.example.carservice.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WorkflowResponse
 *
 * Comprehensive DTO returning vehicle service workflow, booking specs, milestones, and additional repairs.
 */
public class WorkflowResponse {

    private Long id; // Workflow ID
    private Long bookingId;
    private Long vehicleId;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleRegistrationNumber;
    private FuelType vehicleFuelType;
    private Transmission vehicleTransmission;
    private Integer vehicleCurrentMileage;

    private String customerName;
    private String customerEmail;
    private String customerPhone;

    private ServiceType serviceType;
    private String serviceTypeDisplayName;
    private LocalDate bookingDate;
    private TimeSlot timeSlot;
    private String timeSlotLabel;
    private BookingStatus bookingStatus;

    private boolean pickupDropRequired;
    private Integer pickupDropCharge;
    private Integer estimatedServiceAmount;
    private Integer estimatedTotalAmount;

    private WorkflowStatus workflowStatus;
    private String workflowStatusDisplayName;

    private LocalDateTime carReceivedAt;
    private LocalDateTime inspectionStartedAt;
    private LocalDateTime serviceStartedAt;
    private LocalDateTime qualityCheckStartedAt;
    private LocalDateTime readyForDeliveryAt;
    private LocalDateTime completedAt;
    private String notes;
    private LocalDateTime createdAt;

    private List<AdditionalRepairResponse> additionalRepairs = new ArrayList<>();

    public WorkflowResponse() {}

    public static WorkflowResponse fromBookingAndWorkflow(ServiceBooking booking, ServiceWorkflow workflow, List<AdditionalRepair> repairs) {
        WorkflowResponse response = new WorkflowResponse();
        response.setBookingId(booking.getId());
        response.setCreatedAt(booking.getCreatedAt());

        if (booking.getVehicle() != null) {
            response.setVehicleId(booking.getVehicle().getId());
            response.setVehicleMake(booking.getVehicle().getMake());
            response.setVehicleModel(booking.getVehicle().getModel());
            response.setVehicleRegistrationNumber(booking.getVehicle().getRegistrationNumber());
            response.setVehicleFuelType(booking.getVehicle().getFuelType());
            response.setVehicleTransmission(booking.getVehicle().getTransmission());
            response.setVehicleCurrentMileage(booking.getVehicle().getCurrentMileage());
        }

        if (booking.getCustomer() != null) {
            response.setCustomerName(booking.getCustomer().getName());
            response.setCustomerEmail(booking.getCustomer().getEmail());
            response.setCustomerPhone(booking.getCustomer().getPhone());
        }

        response.setServiceType(booking.getServiceType());
        if (booking.getServiceTypesSummary() != null && !booking.getServiceTypesSummary().isBlank()) {
            response.setServiceTypeDisplayName(booking.getServiceTypesSummary());
        } else {
            response.setServiceTypeDisplayName(booking.getServiceType() != null ? booking.getServiceType().getDisplayName() : null);
        }
        response.setBookingDate(booking.getBookingDate());
        response.setTimeSlot(booking.getTimeSlot());
        response.setTimeSlotLabel(booking.getTimeSlot() != null ? booking.getTimeSlot().getLabel() : null);
        response.setBookingStatus(booking.getStatus());

        response.setPickupDropRequired(booking.isPickupDropRequired());
        response.setPickupDropCharge(booking.getPickupDropCharge());
        response.setEstimatedServiceAmount(booking.getEstimatedServiceAmount());
        response.setEstimatedTotalAmount(booking.getEstimatedTotalAmount());

        if (workflow != null) {
            response.setId(workflow.getId());
            response.setWorkflowStatus(workflow.getStatus());
            response.setWorkflowStatusDisplayName(workflow.getStatus() != null ? workflow.getStatus().getDisplayName() : null);
            response.setCarReceivedAt(workflow.getCarReceivedAt());
            response.setInspectionStartedAt(workflow.getInspectionStartedAt());
            response.setServiceStartedAt(workflow.getServiceStartedAt());
            response.setQualityCheckStartedAt(workflow.getQualityCheckStartedAt());
            response.setReadyForDeliveryAt(workflow.getReadyForDeliveryAt());
            response.setCompletedAt(workflow.getCompletedAt());
            response.setNotes(workflow.getNotes());
        }

        if (repairs != null && !repairs.isEmpty()) {
            response.setAdditionalRepairs(
                    repairs.stream().map(AdditionalRepairResponse::fromEntity).collect(Collectors.toList())
            );
        }

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

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public void setVehicleMake(String vehicleMake) {
        this.vehicleMake = vehicleMake;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehicleRegistrationNumber() {
        return vehicleRegistrationNumber;
    }

    public void setVehicleRegistrationNumber(String vehicleRegistrationNumber) {
        this.vehicleRegistrationNumber = vehicleRegistrationNumber;
    }

    public FuelType getVehicleFuelType() {
        return vehicleFuelType;
    }

    public void setVehicleFuelType(FuelType vehicleFuelType) {
        this.vehicleFuelType = vehicleFuelType;
    }

    public Transmission getVehicleTransmission() {
        return vehicleTransmission;
    }

    public void setVehicleTransmission(Transmission vehicleTransmission) {
        this.vehicleTransmission = vehicleTransmission;
    }

    public Integer getVehicleCurrentMileage() {
        return vehicleCurrentMileage;
    }

    public void setVehicleCurrentMileage(Integer vehicleCurrentMileage) {
        this.vehicleCurrentMileage = vehicleCurrentMileage;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceTypeDisplayName() {
        return serviceTypeDisplayName;
    }

    public void setServiceTypeDisplayName(String serviceTypeDisplayName) {
        this.serviceTypeDisplayName = serviceTypeDisplayName;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getTimeSlotLabel() {
        return timeSlotLabel;
    }

    public void setTimeSlotLabel(String timeSlotLabel) {
        this.timeSlotLabel = timeSlotLabel;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public boolean isPickupDropRequired() {
        return pickupDropRequired;
    }

    public void setPickupDropRequired(boolean pickupDropRequired) {
        this.pickupDropRequired = pickupDropRequired;
    }

    public Integer getPickupDropCharge() {
        return pickupDropCharge;
    }

    public void setPickupDropCharge(Integer pickupDropCharge) {
        this.pickupDropCharge = pickupDropCharge;
    }

    public Integer getEstimatedServiceAmount() {
        return estimatedServiceAmount;
    }

    public void setEstimatedServiceAmount(Integer estimatedServiceAmount) {
        this.estimatedServiceAmount = estimatedServiceAmount;
    }

    public Integer getEstimatedTotalAmount() {
        return estimatedTotalAmount;
    }

    public void setEstimatedTotalAmount(Integer estimatedTotalAmount) {
        this.estimatedTotalAmount = estimatedTotalAmount;
    }

    public WorkflowStatus getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(WorkflowStatus workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getWorkflowStatusDisplayName() {
        return workflowStatusDisplayName;
    }

    public void setWorkflowStatusDisplayName(String workflowStatusDisplayName) {
        this.workflowStatusDisplayName = workflowStatusDisplayName;
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

    public List<AdditionalRepairResponse> getAdditionalRepairs() {
        return additionalRepairs;
    }

    public void setAdditionalRepairs(List<AdditionalRepairResponse> additionalRepairs) {
        this.additionalRepairs = additionalRepairs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
