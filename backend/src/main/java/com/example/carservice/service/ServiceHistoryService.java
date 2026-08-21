package com.example.carservice.service;

import com.example.carservice.dto.*;
import com.example.carservice.entity.*;
import com.example.carservice.exception.InvalidWorkflowTransitionException;
import com.example.carservice.exception.ResourceNotFoundException;
import com.example.carservice.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ServiceHistoryService
 *
 * Implements vehicle service record management, itemized completion entry,
 * cost computation, immutability guarantees, and portable PDF generation.
 */
@Service
public class ServiceHistoryService {

    private final ServiceRecordRepository serviceRecordRepository;
    private final ServiceBookingRepository serviceBookingRepository;
    private final ServiceWorkflowRepository serviceWorkflowRepository;
    private final AdditionalRepairRepository additionalRepairRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final PdfReportService pdfReportService;

    public ServiceHistoryService(ServiceRecordRepository serviceRecordRepository,
                                 ServiceBookingRepository serviceBookingRepository,
                                 ServiceWorkflowRepository serviceWorkflowRepository,
                                 AdditionalRepairRepository additionalRepairRepository,
                                 VehicleRepository vehicleRepository,
                                 UserRepository userRepository,
                                 PdfReportService pdfReportService) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.serviceBookingRepository = serviceBookingRepository;
        this.serviceWorkflowRepository = serviceWorkflowRepository;
        this.additionalRepairRepository = additionalRepairRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.pdfReportService = pdfReportService;
    }

    /**
     * Service Center enters/updates service completion details during QUALITY_CHECK or READY_FOR_DELIVERY.
     */
    @Transactional
    public ServiceRecordResponse saveOrUpdateServiceCompletionDetails(Long bookingId, ServiceCompletionRequest request) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidWorkflowTransitionException("Cannot enter service completion details for a cancelled booking.");
        }

        Optional<ServiceRecord> existingOpt = serviceRecordRepository.findByBookingId(bookingId);
        if (existingOpt.isPresent() && existingOpt.get().getFinalizedAt() != null) {
            throw new InvalidWorkflowTransitionException("Finalized historical service records are immutable and cannot be modified.");
        }

        ServiceWorkflow workflow = serviceWorkflowRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service workflow has not been initialized for booking id: " + bookingId));

        if (workflow.getStatus() != WorkflowStatus.QUALITY_CHECK && workflow.getStatus() != WorkflowStatus.READY_FOR_DELIVERY) {
            throw new InvalidWorkflowTransitionException(
                    "Service completion details can only be entered during QUALITY_CHECK or READY_FOR_DELIVERY. Current status: " + workflow.getStatus().name()
            );
        }

        Vehicle vehicle = booking.getVehicle();
        if (request.getMileage() < 0) {
            throw new InvalidWorkflowTransitionException("Service mileage cannot be negative.");
        }
        if (vehicle.getCurrentMileage() != null && request.getMileage() < vehicle.getCurrentMileage()) {
            throw new InvalidWorkflowTransitionException(
                    "Entered mileage (" + request.getMileage() + " km) cannot be lower than the vehicle's current stored mileage (" + vehicle.getCurrentMileage() + " km)."
            );
        }

        ServiceRecord record;

        if (existingOpt.isPresent()) {
            record = existingOpt.get();
            record.getItems().clear();
            record.getInspectionFindings().clear();
        } else {
            record = new ServiceRecord();
            record.setBooking(booking);
            record.setVehicle(vehicle);
            record.setCustomer(booking.getCustomer());
            record.setServiceType(booking.getServiceType());
            record.setServiceTypesSummary(booking.getServiceTypesSummary());
            record.setServiceDate(booking.getBookingDate());
        }

        record.setMileage(request.getMileage());
        record.setServiceSummary(request.getServiceSummary().trim());
        record.setPickupDropUsed(booking.isPickupDropRequired());
        BigDecimal pickupCharge = booking.isPickupDropRequired() && booking.getPickupDropCharge() != null
                ? BigDecimal.valueOf(booking.getPickupDropCharge())
                : BigDecimal.ZERO;
        record.setPickupDropCharge(pickupCharge);

        // 1. Calculate Base Service Items Total
        BigDecimal baseTotal = BigDecimal.ZERO;
        for (ServiceItemDto itemDto : request.getItems()) {
            BigDecimal lineTotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            itemDto.setTotalPrice(lineTotal);
            baseTotal = baseTotal.add(lineTotal);

            ServiceItem item = new ServiceItem(
                    record,
                    itemDto.getDescription().trim(),
                    itemDto.getCategory() != null ? itemDto.getCategory().trim() : "PARTS",
                    itemDto.getQuantity(),
                    itemDto.getUnitPrice(),
                    lineTotal
            );
            record.addItem(item);
        }
        record.setActualBaseServiceAmount(baseTotal);

        // 2. Add Inspection Findings
        for (InspectionFindingDto findingDto : request.getInspectionFindings()) {
            InspectionFinding finding = new InspectionFinding(
                    record,
                    findingDto.getComponent().trim(),
                    findingDto.getConditionStatus().trim(),
                    findingDto.getNotes() != null ? findingDto.getNotes().trim() : null
            );
            record.addInspectionFinding(finding);
        }

        // 3. Calculate Approved Additional Repairs Total (Rejected repairs are excluded from cost)
        List<AdditionalRepair> approvedRepairs = additionalRepairRepository.findByBookingIdAndStatus(bookingId, RepairStatus.APPROVED);
        BigDecimal approvedRepairsTotal = approvedRepairs.stream()
                .map(AdditionalRepair::getEstimatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        record.setActualAdditionalRepairsAmount(approvedRepairsTotal);

        // 4. Calculate Final Actual Total Amount
        BigDecimal actualTotal = baseTotal.add(approvedRepairsTotal).add(pickupCharge);
        record.setActualTotalAmount(actualTotal);

        ServiceRecord saved = serviceRecordRepository.save(record);
        List<AdditionalRepair> allRepairs = additionalRepairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);

        return ServiceRecordResponse.fromEntity(saved, allRepairs);
    }

    /**
     * Retrieves service record for a booking.
     */
    @Transactional(readOnly = true)
    public ServiceRecordResponse getServiceRecordForBooking(Long bookingId, String userEmail) {
        ServiceBooking booking = serviceBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service booking not found with id: " + bookingId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (user.getRole() == Role.CUSTOMER && !booking.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view service records for this booking.");
        }

        ServiceRecord record = serviceRecordRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No service record details found for booking id: " + bookingId));

        List<AdditionalRepair> allRepairs = additionalRepairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);
        return ServiceRecordResponse.fromEntity(record, allRepairs);
    }

    /**
     * Retrieves complete chronological service history for a vehicle.
     */
    @Transactional(readOnly = true)
    public VehicleServiceHistoryResponse getVehicleServiceHistory(Long vehicleId, String userEmail) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (user.getRole() == Role.CUSTOMER && !vehicle.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view service history for this vehicle.");
        }

        List<ServiceRecord> records = serviceRecordRepository.findByVehicleIdAndFinalizedAtIsNotNullOrderByServiceDateDescFinalizedAtDesc(vehicleId);

        List<ServiceRecordResponse> recordResponses = records.stream()
                .map(r -> {
                    List<AdditionalRepair> repairs = additionalRepairRepository.findByBookingIdOrderByRequestedAtDesc(r.getBooking().getId());
                    return ServiceRecordResponse.fromEntity(r, repairs);
                })
                .collect(Collectors.toList());

        return VehicleServiceHistoryResponse.fromVehicleAndRecords(vehicle, recordResponses);
    }

    /**
     * Retrieves a single service record by ID.
     */
    @Transactional(readOnly = true)
    public ServiceRecordResponse getSingleServiceRecord(Long recordId, String userEmail) {
        ServiceRecord record = serviceRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Service record not found with id: " + recordId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (user.getRole() == Role.CUSTOMER && !record.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view this service record.");
        }

        List<AdditionalRepair> repairs = additionalRepairRepository.findByBookingIdOrderByRequestedAtDesc(record.getBooking().getId());
        return ServiceRecordResponse.fromEntity(record, repairs);
    }

    /**
     * Generates a single service visit PDF report.
     */
    @Transactional(readOnly = true)
    public byte[] generateSingleServicePdf(Long recordId, String userEmail) {
        ServiceRecordResponse recordResponse = getSingleServiceRecord(recordId, userEmail);
        return pdfReportService.generateSingleServicePdf(recordResponse);
    }

    /**
     * Generates a complete multi-visit vehicle service passport PDF.
     */
    @Transactional(readOnly = true)
    public byte[] generateVehicleHistoryPdf(Long vehicleId, String userEmail) {
        VehicleServiceHistoryResponse historyResponse = getVehicleServiceHistory(vehicleId, userEmail);
        if (historyResponse.getRecords() == null || historyResponse.getRecords().isEmpty()) {
            throw new ResourceNotFoundException("No completed service history found for vehicle id: " + vehicleId + ". Cannot generate empty PDF.");
        }
        return pdfReportService.generateVehicleHistoryPdf(historyResponse);
    }
}
