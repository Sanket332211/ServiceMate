package com.example.carservice.dto;

import com.example.carservice.entity.BookingStatus;
import com.example.carservice.entity.ServiceBooking;
import com.example.carservice.entity.ServiceType;
import com.example.carservice.entity.TimeSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BookingResponse
 *
 * Data Transfer Object returned to the client when querying service bookings.
 */
public class BookingResponse {

    private Long id;
    private Long vehicleId;
    private String vehicleMake;
    private String vehicleModel;
    private String vehicleRegistrationNumber;
    private ServiceType serviceType;
    private String serviceTypeDisplayName;
    private LocalDate bookingDate;
    private TimeSlot timeSlot;
    private String timeSlotLabel;
    private BookingStatus status;
    private boolean pickupDropRequired;
    private Integer pickupDropCharge;
    private Integer estimatedServiceAmount;
    private Integer estimatedTotalAmount;
    private LocalDateTime createdAt;

    public BookingResponse() {}

    public static BookingResponse fromEntity(ServiceBooking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        if (booking.getVehicle() != null) {
            response.setVehicleId(booking.getVehicle().getId());
            response.setVehicleMake(booking.getVehicle().getMake());
            response.setVehicleModel(booking.getVehicle().getModel());
            response.setVehicleRegistrationNumber(booking.getVehicle().getRegistrationNumber());
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
        response.setStatus(booking.getStatus());
        response.setPickupDropRequired(booking.isPickupDropRequired());
        response.setPickupDropCharge(booking.getPickupDropCharge());
        response.setEstimatedServiceAmount(booking.getEstimatedServiceAmount());
        response.setEstimatedTotalAmount(booking.getEstimatedTotalAmount());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
