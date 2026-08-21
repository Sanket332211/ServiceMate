package com.example.carservice.dto;

import com.example.carservice.entity.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ServiceRecordResponse
 *
 * Detailed DTO representing a vehicle service visit, including work items, inspection findings,
 * approved/rejected additional repairs, and actual cost calculations.
 */
public class ServiceRecordResponse {

    private Long id;
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
    private LocalDate serviceDate;
    private Integer mileage;
    private String serviceSummary;

    private BigDecimal actualBaseServiceAmount;
    private BigDecimal actualAdditionalRepairsAmount;
    private boolean pickupDropUsed;
    private BigDecimal pickupDropCharge;
    private BigDecimal actualTotalAmount;

    private LocalDateTime createdAt;
    private LocalDateTime finalizedAt;

    private List<ServiceItemDto> items = new ArrayList<>();
    private List<InspectionFindingDto> inspectionFindings = new ArrayList<>();
    private List<AdditionalRepairResponse> additionalRepairs = new ArrayList<>();

    public ServiceRecordResponse() {}

    public static ServiceRecordResponse fromEntity(ServiceRecord record, List<AdditionalRepair> repairs) {
        ServiceRecordResponse response = new ServiceRecordResponse();
        response.setId(record.getId());
        if (record.getBooking() != null) {
            response.setBookingId(record.getBooking().getId());
        }

        if (record.getVehicle() != null) {
            response.setVehicleId(record.getVehicle().getId());
            response.setVehicleMake(record.getVehicle().getMake());
            response.setVehicleModel(record.getVehicle().getModel());
            response.setVehicleRegistrationNumber(record.getVehicle().getRegistrationNumber());
            response.setVehicleFuelType(record.getVehicle().getFuelType());
            response.setVehicleTransmission(record.getVehicle().getTransmission());
            response.setVehicleCurrentMileage(record.getVehicle().getCurrentMileage());
        }

        if (record.getCustomer() != null) {
            response.setCustomerName(record.getCustomer().getName());
            response.setCustomerEmail(record.getCustomer().getEmail());
            response.setCustomerPhone(record.getCustomer().getPhone());
        }

        response.setServiceType(record.getServiceType());
        if (record.getServiceTypesSummary() != null && !record.getServiceTypesSummary().isBlank()) {
            response.setServiceTypeDisplayName(record.getServiceTypesSummary());
        } else {
            response.setServiceTypeDisplayName(record.getServiceType() != null ? record.getServiceType().getDisplayName() : null);
        }
        response.setServiceDate(record.getServiceDate());
        response.setMileage(record.getMileage());
        response.setServiceSummary(record.getServiceSummary());

        response.setActualBaseServiceAmount(record.getActualBaseServiceAmount());
        response.setActualAdditionalRepairsAmount(record.getActualAdditionalRepairsAmount());
        response.setPickupDropUsed(record.isPickupDropUsed());
        response.setPickupDropCharge(record.getPickupDropCharge());
        response.setActualTotalAmount(record.getActualTotalAmount());

        response.setCreatedAt(record.getCreatedAt());
        response.setFinalizedAt(record.getFinalizedAt());

        if (record.getItems() != null) {
            response.setItems(record.getItems().stream().map(ServiceItemDto::fromEntity).collect(Collectors.toList()));
        }

        if (record.getInspectionFindings() != null) {
            response.setInspectionFindings(
                    record.getInspectionFindings().stream().map(InspectionFindingDto::fromEntity).collect(Collectors.toList())
            );
        }

        if (repairs != null) {
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

    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
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

    public BigDecimal getActualBaseServiceAmount() {
        return actualBaseServiceAmount;
    }

    public void setActualBaseServiceAmount(BigDecimal actualBaseServiceAmount) {
        this.actualBaseServiceAmount = actualBaseServiceAmount;
    }

    public BigDecimal getActualAdditionalRepairsAmount() {
        return actualAdditionalRepairsAmount;
    }

    public void setActualAdditionalRepairsAmount(BigDecimal actualAdditionalRepairsAmount) {
        this.actualAdditionalRepairsAmount = actualAdditionalRepairsAmount;
    }

    public boolean isPickupDropUsed() {
        return pickupDropUsed;
    }

    public void setPickupDropUsed(boolean pickupDropUsed) {
        this.pickupDropUsed = pickupDropUsed;
    }

    public BigDecimal getPickupDropCharge() {
        return pickupDropCharge;
    }

    public void setPickupDropCharge(BigDecimal pickupDropCharge) {
        this.pickupDropCharge = pickupDropCharge;
    }

    public BigDecimal getActualTotalAmount() {
        return actualTotalAmount;
    }

    public void setActualTotalAmount(BigDecimal actualTotalAmount) {
        this.actualTotalAmount = actualTotalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getFinalizedAt() {
        return finalizedAt;
    }

    public void setFinalizedAt(LocalDateTime finalizedAt) {
        this.finalizedAt = finalizedAt;
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

    public List<AdditionalRepairResponse> getAdditionalRepairs() {
        return additionalRepairs;
    }

    public void setAdditionalRepairs(List<AdditionalRepairResponse> additionalRepairs) {
        this.additionalRepairs = additionalRepairs;
    }
}
