package com.example.carservice.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ServiceBooking
 *
 * Entity representing a capacity-controlled car service appointment in ServiceMate.
 * Maps to the `service_bookings` table in MySQL.
 */
@Entity
@Table(name = "service_bookings")
public class ServiceBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 40)
    private ServiceType serviceType;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot", nullable = false, length = 40)
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookingStatus status;

    @Column(name = "pickup_drop_required", nullable = false)
    private boolean pickupDropRequired;

    @Column(name = "pickup_drop_charge", nullable = false)
    private Integer pickupDropCharge;

    @Column(name = "estimated_service_amount", nullable = false)
    private Integer estimatedServiceAmount;

    @Column(name = "estimated_total_amount", nullable = false)
    private Integer estimatedTotalAmount;

    @Column(name = "service_types_summary", length = 255)
    private String serviceTypesSummary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ServiceBooking() {
        this.createdAt = LocalDateTime.now();
    }

    public ServiceBooking(User customer, Vehicle vehicle, ServiceType serviceType, LocalDate bookingDate,
                          TimeSlot timeSlot, BookingStatus status, boolean pickupDropRequired,
                          Integer pickupDropCharge, Integer estimatedServiceAmount, Integer estimatedTotalAmount) {
        this(customer, vehicle, serviceType, null, bookingDate, timeSlot, status, pickupDropRequired, pickupDropCharge, estimatedServiceAmount, estimatedTotalAmount);
    }

    public ServiceBooking(User customer, Vehicle vehicle, ServiceType serviceType, String serviceTypesSummary,
                          LocalDate bookingDate, TimeSlot timeSlot, BookingStatus status, boolean pickupDropRequired,
                          Integer pickupDropCharge, Integer estimatedServiceAmount, Integer estimatedTotalAmount) {
        this.customer = customer;
        this.vehicle = vehicle;
        this.serviceType = serviceType;
        this.serviceTypesSummary = serviceTypesSummary;
        this.bookingDate = bookingDate;
        this.timeSlot = timeSlot;
        this.status = status;
        this.pickupDropRequired = pickupDropRequired;
        this.pickupDropCharge = pickupDropCharge;
        this.estimatedServiceAmount = estimatedServiceAmount;
        this.estimatedTotalAmount = estimatedTotalAmount;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
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

    public String getServiceTypesSummary() {
        return serviceTypesSummary;
    }

    public void setServiceTypesSummary(String serviceTypesSummary) {
        this.serviceTypesSummary = serviceTypesSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
