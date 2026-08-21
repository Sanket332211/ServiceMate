package com.example.carservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Vehicle Entity
 *
 * Represents the 'vehicles' table in MySQL.
 * Models a car owned by a registered customer in the 1-to-Many (User -> Vehicles) relationship.
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Vehicle must have an associated owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @NotBlank(message = "Registration number is required")
    @Size(min = 4, max = 20, message = "Registration number must be between 4 and 20 characters")
    @Column(name = "registration_number", nullable = false, unique = true, length = 30)
    private String registrationNumber;

    @NotBlank(message = "Make is required")
    @Size(min = 2, max = 60, message = "Make must be between 2 and 60 characters")
    @Column(nullable = false, length = 60)
    private String make;

    @NotBlank(message = "Model is required")
    @Size(min = 1, max = 60, message = "Model must be between 1 and 60 characters")
    @Column(nullable = false, length = 60)
    private String model;

    @NotNull(message = "Manufacturing year is required")
    @Min(value = 1980, message = "Manufacturing year must be 1980 or later")
    @Column(name = "manufacturing_year", nullable = false)
    private Integer manufacturingYear;

    @NotNull(message = "Fuel type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 30)
    private FuelType fuelType;

    @NotNull(message = "Transmission is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Transmission transmission;

    @Min(value = 0, message = "Current mileage cannot be negative")
    @Column(name = "current_mileage")
    private Integer currentMileage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public Vehicle() {
    }

    public Vehicle(User owner, String registrationNumber, String make, String model,
                   Integer manufacturingYear, FuelType fuelType, Transmission transmission,
                   Integer currentMileage) {
        this.owner = owner;
        this.registrationNumber = registrationNumber != null ? registrationNumber.trim().toUpperCase() : null;
        this.make = make != null ? make.trim() : null;
        this.model = model != null ? model.trim() : null;
        this.manufacturingYear = manufacturingYear;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.currentMileage = currentMileage != null ? currentMileage : 0;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.registrationNumber != null) {
            this.registrationNumber = this.registrationNumber.trim().toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.registrationNumber != null) {
            this.registrationNumber = this.registrationNumber.trim().toUpperCase();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber != null ? registrationNumber.trim().toUpperCase() : null;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make != null ? make.trim() : null;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model != null ? model.trim() : null;
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
