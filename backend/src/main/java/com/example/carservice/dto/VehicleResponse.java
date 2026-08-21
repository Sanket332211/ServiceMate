package com.example.carservice.dto;

import com.example.carservice.entity.FuelType;
import com.example.carservice.entity.Transmission;
import com.example.carservice.entity.Vehicle;

import java.time.LocalDateTime;

/**
 * VehicleResponse DTO
 *
 * Returned to the client representing vehicle details along with safe owner metadata.
 */
public class VehicleResponse {

    private Long id;
    private Long ownerId;
    private String ownerName;
    private String ownerEmail;
    private String registrationNumber;
    private String make;
    private String model;
    private Integer manufacturingYear;
    private FuelType fuelType;
    private Transmission transmission;
    private Integer currentMileage;
    private LocalDateTime createdAt;

    public VehicleResponse() {
    }

    public VehicleResponse(Vehicle vehicle) {
        this.id = vehicle.getId();
        if (vehicle.getOwner() != null) {
            this.ownerId = vehicle.getOwner().getId();
            this.ownerName = vehicle.getOwner().getName();
            this.ownerEmail = vehicle.getOwner().getEmail();
        }
        this.registrationNumber = vehicle.getRegistrationNumber();
        this.make = vehicle.getMake();
        this.model = vehicle.getModel();
        this.manufacturingYear = vehicle.getManufacturingYear();
        this.fuelType = vehicle.getFuelType();
        this.transmission = vehicle.getTransmission();
        this.currentMileage = vehicle.getCurrentMileage();
        this.createdAt = vehicle.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getManufacturingYear() {
        return manufacturingYear;
    }

    public void setManufacturingYear(Integer manufacturingYear) {
        this.manufacturingYear = manufacturingYear;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public Transmission getTransmission() {
        return transmission;
    }

    public void setTransmission(Transmission transmission) {
        this.transmission = transmission;
    }

    public Integer getCurrentMileage() {
        return currentMileage;
    }

    public void setCurrentMileage(Integer currentMileage) {
        this.currentMileage = currentMileage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
