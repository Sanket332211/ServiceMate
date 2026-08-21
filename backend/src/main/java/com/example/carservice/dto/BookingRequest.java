package com.example.carservice.dto;

import com.example.carservice.entity.ServiceType;
import com.example.carservice.entity.TimeSlot;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

/**
 * BookingRequest
 *
 * Data Transfer Object for creating a new service booking.
 * Supports single service package or multiple selected packages.
 * Note: Pricing, status, and customer are calculated on the backend to prevent tampering.
 */
public class BookingRequest {

    @NotNull(message = "Vehicle ID is required.")
    private Long vehicleId;

    private ServiceType serviceType;

    private List<ServiceType> serviceTypes = new ArrayList<>();

    @NotNull(message = "Booking date is required.")
    private LocalDate bookingDate;

    @NotNull(message = "Time slot is required.")
    private TimeSlot timeSlot;

    private boolean pickupDropRequired;

    public BookingRequest() {}

    public BookingRequest(Long vehicleId, ServiceType serviceType, LocalDate bookingDate, TimeSlot timeSlot, boolean pickupDropRequired) {
        this.vehicleId = vehicleId;
        this.serviceType = serviceType;
        if (serviceType != null) {
            this.serviceTypes.add(serviceType);
        }
        this.bookingDate = bookingDate;
        this.timeSlot = timeSlot;
        this.pickupDropRequired = pickupDropRequired;
    }

    public BookingRequest(Long vehicleId, List<ServiceType> serviceTypes, LocalDate bookingDate, TimeSlot timeSlot, boolean pickupDropRequired) {
        this.vehicleId = vehicleId;
        this.serviceTypes = serviceTypes != null ? serviceTypes : new ArrayList<>();
        if (!this.serviceTypes.isEmpty()) {
            this.serviceType = this.serviceTypes.get(0);
        }
        this.bookingDate = bookingDate;
        this.timeSlot = timeSlot;
        this.pickupDropRequired = pickupDropRequired;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public List<ServiceType> getResolvedServiceTypes() {
        if (serviceTypes != null && !serviceTypes.isEmpty()) {
            return new ArrayList<>(new java.util.LinkedHashSet<>(serviceTypes));
        }
        if (serviceType != null) {
            return List.of(serviceType);
        }
        return List.of();
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public List<ServiceType> getServiceTypes() {
        return serviceTypes;
    }

    public void setServiceTypes(List<ServiceType> serviceTypes) {
        this.serviceTypes = serviceTypes;
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

    public boolean isPickupDropRequired() {
        return pickupDropRequired;
    }

    public void setPickupDropRequired(boolean pickupDropRequired) {
        this.pickupDropRequired = pickupDropRequired;
    }
}
