package com.example.carservice.dto;

import com.example.carservice.entity.FuelType;
import com.example.carservice.entity.Transmission;
import com.example.carservice.entity.Vehicle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * VehicleServiceHistoryResponse
 *
 * Aggregate DTO for a vehicle's complete chronological service passport history.
 */
public class VehicleServiceHistoryResponse {

    private Long vehicleId;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleRegistrationNumber;
    private FuelType vehicleFuelType;
    private Transmission vehicleTransmission;
    private Integer currentMileage;
    private String customerName;

    private int totalCompletedVisits;
    private BigDecimal totalAmountSpent;

    private List<ServiceRecordResponse> records = new ArrayList<>();

    public VehicleServiceHistoryResponse() {
        this.totalAmountSpent = BigDecimal.ZERO;
    }

    public static VehicleServiceHistoryResponse fromVehicleAndRecords(Vehicle vehicle, List<ServiceRecordResponse> records) {
        VehicleServiceHistoryResponse response = new VehicleServiceHistoryResponse();
        if (vehicle != null) {
            response.setVehicleId(vehicle.getId());
            response.setVehicleMake(vehicle.getMake());
            response.setVehicleModel(vehicle.getModel());
            response.setVehicleRegistrationNumber(vehicle.getRegistrationNumber());
            response.setVehicleFuelType(vehicle.getFuelType());
            response.setVehicleTransmission(vehicle.getTransmission());
            response.setCurrentMileage(vehicle.getCurrentMileage());
            if (vehicle.getOwner() != null) {
                response.setCustomerName(vehicle.getOwner().getName());
            }
        }

        if (records != null) {
            response.setRecords(records);
            response.setTotalCompletedVisits(records.size());
            BigDecimal total = records.stream()
                    .map(ServiceRecordResponse::getActualTotalAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.setTotalAmountSpent(total);
        }

        return response;
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

    public Integer getCurrentMileage() {
        return currentMileage;
    }

    public void setCurrentMileage(Integer currentMileage) {
        this.currentMileage = currentMileage;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getTotalCompletedVisits() {
        return totalCompletedVisits;
    }

    public void setTotalCompletedVisits(int totalCompletedVisits) {
        this.totalCompletedVisits = totalCompletedVisits;
    }

    public BigDecimal getTotalAmountSpent() {
        return totalAmountSpent;
    }

    public void setTotalAmountSpent(BigDecimal totalAmountSpent) {
        this.totalAmountSpent = totalAmountSpent;
    }

    public List<ServiceRecordResponse> getRecords() {
        return records;
    }

    public void setRecords(List<ServiceRecordResponse> records) {
        this.records = records;
    }
}
