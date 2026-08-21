package com.example.carservice.dto;

import com.example.carservice.entity.FuelType;
import com.example.carservice.entity.Transmission;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * VehicleRequest DTO
 *
 * Encapsulates the payload sent from the client when adding or editing a vehicle.
 */
public class VehicleRequest {

    @NotBlank(message = "Registration number is required")
    @Size(min = 4, max = 20, message = "Registration number must be between 4 and 20 characters")
    private String registrationNumber;

    @NotBlank(message = "Make is required")
    @Size(min = 2, max = 60, message = "Make must be between 2 and 60 characters")
    private String make;

    @NotBlank(message = "Model is required")
    @Size(min = 1, max = 60, message = "Model must be between 1 and 60 characters")
    private String model;

    @NotNull(message = "Manufacturing year is required")
    @Min(value = 1980, message = "Manufacturing year must be 1980 or later")
    private Integer manufacturingYear;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull(message = "Transmission is required")
    private Transmission transmission;

    @Min(value = 0, message = "Current mileage cannot be negative")
    private Integer currentMileage;

    public VehicleRequest() {
    }

    public VehicleRequest(String registrationNumber, String make, String model,
                          Integer manufacturingYear, FuelType fuelType,
                          Transmission transmission, Integer currentMileage) {
        this.registrationNumber = registrationNumber;
        this.make = make;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.currentMileage = currentMileage;
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
}
