package com.example.carservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ServiceRecord
 *
 * Represents the finalized, immutable historical service passport record for a vehicle visit.
 */
@Entity
@Table(name = "service_records")
public class ServiceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private ServiceBooking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    @Column(name = "service_types_summary", length = 255)
    private String serviceTypesSummary;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false)
    private Integer mileage;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String serviceSummary;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal actualBaseServiceAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal actualAdditionalRepairsAmount;

    @Column(nullable = false)
    private boolean pickupDropUsed;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pickupDropCharge;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal actualTotalAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime finalizedAt;

    @OneToMany(mappedBy = "serviceRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "serviceRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InspectionFinding> inspectionFindings = new ArrayList<>();

    public ServiceRecord() {
        this.createdAt = LocalDateTime.now();
        this.actualBaseServiceAmount = BigDecimal.ZERO;
        this.actualAdditionalRepairsAmount = BigDecimal.ZERO;
        this.pickupDropCharge = BigDecimal.ZERO;
        this.actualTotalAmount = BigDecimal.ZERO;
    }

    public ServiceRecord(ServiceBooking booking, Vehicle vehicle, User customer,
                         ServiceType serviceType, LocalDate serviceDate, Integer mileage,
                         String serviceSummary, BigDecimal actualBaseServiceAmount,
                         BigDecimal actualAdditionalRepairsAmount, boolean pickupDropUsed,
                         BigDecimal pickupDropCharge, BigDecimal actualTotalAmount) {
        this.booking = booking;
        this.vehicle = vehicle;
        this.customer = customer;
        this.serviceType = serviceType;
        this.serviceDate = serviceDate;
        this.mileage = mileage;
        this.serviceSummary = serviceSummary;
        this.actualBaseServiceAmount = actualBaseServiceAmount;
        this.actualAdditionalRepairsAmount = actualAdditionalRepairsAmount;
        this.pickupDropUsed = pickupDropUsed;
        this.pickupDropCharge = pickupDropCharge;
        this.actualTotalAmount = actualTotalAmount;
        this.createdAt = LocalDateTime.now();
    }

    public void addItem(ServiceItem item) {
        items.add(item);
        item.setServiceRecord(this);
    }

    public void removeItem(ServiceItem item) {
        items.remove(item);
        item.setServiceRecord(null);
    }

    public void addInspectionFinding(InspectionFinding finding) {
        inspectionFindings.add(finding);
        finding.setServiceRecord(this);
    }

    public void removeInspectionFinding(InspectionFinding finding) {
        inspectionFindings.remove(finding);
        finding.setServiceRecord(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServiceBooking getBooking() {
        return booking;
    }

    public void setBooking(ServiceBooking booking) {
        this.booking = booking;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
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

    public List<ServiceItem> getItems() {
        return items;
    }

    public void setItems(List<ServiceItem> items) {
        this.items = items;
    }

    public List<InspectionFinding> getInspectionFindings() {
        return inspectionFindings;
    }

    public void setInspectionFindings(List<InspectionFinding> inspectionFindings) {
        this.inspectionFindings = inspectionFindings;
    }

    public String getServiceTypesSummary() {
        return serviceTypesSummary;
    }

    public void setServiceTypesSummary(String serviceTypesSummary) {
        this.serviceTypesSummary = serviceTypesSummary;
    }
}
