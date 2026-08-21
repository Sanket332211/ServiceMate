package com.example.carservice.service;

import com.example.carservice.dto.*;
import com.example.carservice.entity.*;
import com.example.carservice.exception.InvalidRepairStateException;
import com.example.carservice.exception.InvalidWorkflowTransitionException;
import com.example.carservice.exception.ResourceNotFoundException;
import com.example.carservice.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ServiceWorkflowService
 *
 * Implements the workshop service execution state machine and additional repair authorization logic:
 * - Workflow milestones: CAR_RECEIVED -> INSPECTION -> SERVICE_IN_PROGRESS -> QUALITY_CHECK -> READY_FOR_DELIVERY -> COMPLETED
 * - Additional repair lifecycle: PENDING -> APPROVED / REJECTED (Customer-only authorization)
 * - Automated in-app notifications and WebSocket real-time updates
 */
@Service
public class ServiceWorkflowService {

    private final ServiceWorkflowRepository workflowRepository;
    private final ServiceBookingRepository bookingRepository;
    private final AdditionalRepairRepository repairRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WorkflowWebSocketPublisher wsPublisher;
    private final ServiceRecordRepository serviceRecordRepository;
    private final VehicleRepository vehicleRepository;

    public ServiceWorkflowService(ServiceWorkflowRepository workflowRepository,
                                  ServiceBookingRepository bookingRepository,
                                  AdditionalRepairRepository repairRepository,
                                  UserRepository userRepository,
                                  NotificationService notificationService,
                                  WorkflowWebSocketPublisher wsPublisher,
                                  ServiceRecordRepository serviceRecordRepository,
                                  VehicleRepository vehicleRepository) {
        this.workflowRepository = workflowRepository;
        this.bookingRepository = bookingRepository;
        this.repairRepository = repairRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.wsPublisher = wsPublisher;
        this.serviceRecordRepository = serviceRecordRepository;
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * CONFIRMED -> CAR_RECEIVED
     * Marks a customer's vehicle as received at the service center and initializes the workflow.
     */
    @Transactional
    public WorkflowResponse receiveVehicle(Long bookingId, String notes) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidWorkflowTransitionException("Cancelled bookings cannot enter the service workflow.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidWorkflowTransitionException("Only confirmed bookings can be marked as received.");
        }

        Optional<ServiceWorkflow> existingOpt = workflowRepository.findByBookingId(bookingId);
        ServiceWorkflow workflow;

        if (existingOpt.isPresent()) {
            workflow = existingOpt.get();
            if (workflow.getStatus() != WorkflowStatus.CAR_RECEIVED) {
                throw new InvalidWorkflowTransitionException("Vehicle has already progressed beyond CAR_RECEIVED to " + workflow.getStatus().name());
            }
        } else {
            workflow = new ServiceWorkflow(booking, WorkflowStatus.CAR_RECEIVED);
            workflow.setCarReceivedAt(LocalDateTime.now());
        }

        if (notes != null && !notes.isBlank()) {
            workflow.setNotes(notes);
        }

        ServiceWorkflow saved = workflowRepository.save(workflow);

        // Notify customer
        notifyAndBroadcast(
                booking.getCustomer(),
                "Car Received at Workshop",
                "Your " + booking.getVehicle().getMake() + " " + booking.getVehicle().getModel() + " has arrived at ServiceMate.",
                NotificationType.SERVICE_STATUS_UPDATED,
                bookingId,
                WorkflowStatus.CAR_RECEIVED
        );

        List<AdditionalRepair> repairs = repairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);
        return WorkflowResponse.fromBookingAndWorkflow(booking, saved, repairs);
    }

    /**
     * CAR_RECEIVED -> INSPECTION
     */
    @Transactional
    public WorkflowResponse startInspection(Long bookingId, String notes) {
        ServiceWorkflow workflow = getWorkflowOrThrow(bookingId);

        if (workflow.getStatus() != WorkflowStatus.CAR_RECEIVED) {
            throw new InvalidWorkflowTransitionException("Vehicle must be in CAR_RECEIVED state before starting inspection. Current state: " + workflow.getStatus().name());
        }

        workflow.setStatus(WorkflowStatus.INSPECTION);
        workflow.setInspectionStartedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            workflow.setNotes(notes);
        }

        ServiceWorkflow saved = workflowRepository.save(workflow);
        ServiceBooking booking = workflow.getBooking();

        notifyAndBroadcast(
                booking.getCustomer(),
                "Inspection in Progress",
                "Our certified technicians have begun the 40-point vehicle inspection for your " + booking.getVehicle().getModel() + ".",
                NotificationType.SERVICE_STATUS_UPDATED,
                bookingId,
                WorkflowStatus.INSPECTION
        );

        List<AdditionalRepair> repairs = repairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);
        return WorkflowResponse.fromBookingAndWorkflow(booking, saved, repairs);
    }

    /**
     * INSPECTION -> SERVICE_IN_PROGRESS
     */
    @Transactional
    public WorkflowResponse startService(Long bookingId, String notes) {
        ServiceWorkflow workflow = getWorkflowOrThrow(bookingId);

        if (workflow.getStatus() != WorkflowStatus.INSPECTION) {
            throw new InvalidWorkflowTransitionException("Vehicle must be in INSPECTION state before starting service. Current state: " + workflow.getStatus().name());
        }

        workflow.setStatus(WorkflowStatus.SERVICE_IN_PROGRESS);
        workflow.setServiceStartedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            workflow.setNotes(notes);
        }

        ServiceWorkflow saved = workflowRepository.save(workflow);
        ServiceBooking booking = workflow.getBooking();

        notifyAndBroadcast(
                booking.getCustomer(),
                "Service in Progress",
                "Service & maintenance work has commenced on your " + booking.getVehicle().getModel() + ".",
                NotificationType.SERVICE_STATUS_UPDATED,
                bookingId,
                WorkflowStatus.SERVICE_IN_PROGRESS
        );

        List<AdditionalRepair> repairs = repairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);
        return WorkflowResponse.fromBookingAndWorkflow(booking, saved, repairs);
    }

    /**
     * SERVICE_IN_PROGRESS -> QUALITY_CHECK
     * Can only advance if no additional repairs remain in PENDING state.
     */
    @Transactional
    public WorkflowResponse startQualityCheck(Long bookingId, String notes) {
        ServiceWorkflow workflow = getWorkflowOrThrow(bookingId);

        long pendingRepairs = repairRepository.countByBookingIdAndStatus(bookingId, RepairStatus.PENDING);
        if (pendingRepairs > 0) {
            throw new InvalidWorkflowTransitionException("Cannot move to QUALITY_CHECK while " + pendingRepairs + " additional repair request(s) are awaiting customer approval.");
        }

        if (workflow.getStatus() != WorkflowStatus.SERVICE_IN_PROGRESS && workflow.getStatus() != WorkflowStatus.AWAITING_APPROVAL) {
            throw new InvalidWorkflowTransitionException("Invalid transition to QUALITY_CHECK from current status: " + workflow.getStatus().name());
        }

        workflow.setStatus(WorkflowStatus.QUALITY_CHECK);
        workflow.setQualityCheckStartedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            workflow.setNotes(notes);
        }

        ServiceWorkflow saved = workflowRepository.save(workflow);
        ServiceBooking booking = workflow.getBooking();

        notifyAndBroadcast(
                booking.getCustomer(),
                "Quality Check & Road Test",
                "Service work is complete. Your vehicle is currently undergoing final quality inspection and testing.",
                NotificationType.SERVICE_STATUS_UPDATED,
                bookingId,
                WorkflowStatus.QUALITY_CHECK
        );

        List<AdditionalRepair> repairs = repairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);
        return WorkflowResponse.fromBookingAndWorkflow(booking, saved, repairs);
    }

    /**
     * QUALITY_CHECK -> READY_FOR_DELIVERY
     * Requires valid ServiceRecord completion details to exist before advancing.
     */
    @Transactional
    public WorkflowResponse markReadyForDelivery(Long bookingId, String notes) {
        ServiceWorkflow workflow = getWorkflowOrThrow(bookingId);

        if (workflow.getStatus() != WorkflowStatus.QUALITY_CHECK) {
            throw new InvalidWorkflowTransitionException("Vehicle must pass QUALITY_CHECK before being marked READY_FOR_DELIVERY. Current status: " + workflow.getStatus().name());
        }

        // Verify service completion details exist
        ServiceRecord serviceRecord = serviceRecordRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InvalidWorkflowTransitionException("Service completion details are incomplete. Please enter mileage, summary, service items, and inspection findings before marking ready for delivery."));

        if (serviceRecord.getMileage() == null ||
                serviceRecord.getServiceSummary() == null || serviceRecord.getServiceSummary().isBlank() ||
                serviceRecord.getItems() == null || serviceRecord.getItems().isEmpty() ||
                serviceRecord.getInspectionFindings() == null || serviceRecord.getInspectionFindings().isEmpty()) {
            throw new InvalidWorkflowTransitionException("Service completion details are incomplete. Please enter mileage, summary, service items, and inspection findings before marking ready for delivery.");
        }

        workflow.setStatus(WorkflowStatus.READY_FOR_DELIVERY);
        workflow.setReadyForDeliveryAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            workflow.setNotes(notes);
        }

        ServiceWorkflow saved = workflowRepository.save(workflow);
        ServiceBooking booking = workflow.getBooking();

        notifyAndBroadcast(
                booking.getCustomer(),
                "Vehicle Ready for Handover",
                "Your " + booking.getVehicle().getMake() + " " + booking.getVehicle().getModel() + " is ready for pickup/delivery!",
                NotificationType.VEHICLE_READY,
                bookingId,
                WorkflowStatus.READY_FOR_DELIVERY
        );

        List<AdditionalRepair> repairs = repairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);
        return WorkflowResponse.fromBookingAndWorkflow(booking, saved, repairs);
    }

    /**
     * READY_FOR_DELIVERY -> COMPLETED
     * Finalizes ServiceRecord, updates Vehicle odometer mileage, and marks ServiceBooking as COMPLETED.
     */
    @Transactional
    public WorkflowResponse completeService(Long bookingId, String notes) {
        ServiceWorkflow workflow = getWorkflowOrThrow(bookingId);

        if (workflow.getStatus() != WorkflowStatus.READY_FOR_DELIVERY) {
            throw new InvalidWorkflowTransitionException("Vehicle must be in READY_FOR_DELIVERY before completing service. Current status: " + workflow.getStatus().name());
        }

        workflow.setStatus(WorkflowStatus.COMPLETED);
        workflow.setCompletedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            workflow.setNotes(notes);
        }

        // Finalize ServiceRecord and update Vehicle currentMileage
        Optional<ServiceRecord> recordOpt = serviceRecordRepository.findByBookingId(bookingId);
        if (recordOpt.isPresent()) {
            ServiceRecord record = recordOpt.get();
            if (record.getFinalizedAt() == null) {
                record.setFinalizedAt(LocalDateTime.now());
                serviceRecordRepository.save(record);
            }
            if (record.getMileage() != null) {
                Vehicle vehicle = workflow.getBooking().getVehicle();
                vehicle.setCurrentMileage(record.getMileage());
                vehicleRepository.save(vehicle);
            }
        }

        // Update associated booking status to COMPLETED
        ServiceBooking booking = workflow.getBooking();
        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        ServiceWorkflow saved = workflowRepository.save(workflow);

        notifyAndBroadcast(
                booking.getCustomer(),
                "Service Completed",
                "Your vehicle service has been successfully completed and closed. Thank you for choosing ServiceMate!",
                NotificationType.SERVICE_COMPLETED,
                bookingId,
                WorkflowStatus.COMPLETED
        );

        List<AdditionalRepair> repairs = repairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);
        return WorkflowResponse.fromBookingAndWorkflow(booking, saved, repairs);
    }

    /**
     * Creates an additional repair finding during SERVICE_IN_PROGRESS.
     * Transitions workflow to AWAITING_APPROVAL.
     */
    @Transactional
    public AdditionalRepairResponse createAdditionalRepair(Long bookingId, AdditionalRepairRequest request) {
        ServiceWorkflow workflow = getWorkflowOrThrow(bookingId);

        if (workflow.getStatus() != WorkflowStatus.SERVICE_IN_PROGRESS && workflow.getStatus() != WorkflowStatus.AWAITING_APPROVAL) {
            throw new InvalidWorkflowTransitionException("Additional repairs can only be created during SERVICE_IN_PROGRESS or AWAITING_APPROVAL. Current status: " + workflow.getStatus().name());
        }

        ServiceBooking booking = workflow.getBooking();
        AdditionalRepair repair = new AdditionalRepair(
                booking,
                request.getDescription().trim(),
                request.getReason().trim(),
                request.getEstimatedAmount()
        );

        AdditionalRepair savedRepair = repairRepository.save(repair);

        // Transition workflow to AWAITING_APPROVAL
        workflow.setStatus(WorkflowStatus.AWAITING_APPROVAL);
        workflowRepository.save(workflow);

        // Notify customer
        notifyAndBroadcast(
                booking.getCustomer(),
                "Additional Repair Approval Required",
                "Inspection discovered: " + request.getDescription() + " (Est: ₹" + request.getEstimatedAmount() + "). Please review and authorize.",
                NotificationType.REPAIR_REQUESTED,
                bookingId,
                WorkflowStatus.AWAITING_APPROVAL
        );

        return AdditionalRepairResponse.fromEntity(savedRepair);
    }

    /**
     * Customer authorizes an additional repair request.
     * If all pending repairs for the booking are resolved:
     * - Transitions workflow back to SERVICE_IN_PROGRESS.
     */
    @Transactional
    public AdditionalRepairResponse approveAdditionalRepair(Long repairId, String customerEmail) {
        AdditionalRepair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new ResourceNotFoundException("Additional repair request not found with id: " + repairId));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));

        if (!repair.getBooking().getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to authorize repairs for this vehicle.");
        }

        if (repair.getStatus() != RepairStatus.PENDING) {
            throw new InvalidRepairStateException("This repair request has already been " + repair.getStatus().name() + " and cannot be modified.");
        }

        repair.setStatus(RepairStatus.APPROVED);
        repair.setRespondedAt(LocalDateTime.now());
        AdditionalRepair updatedRepair = repairRepository.save(repair);

        // Check remaining pending repairs for this booking
        long remainingPending = repairRepository.countByBookingIdAndStatus(repair.getBooking().getId(), RepairStatus.PENDING);
        ServiceWorkflow workflow = getWorkflowOrThrow(repair.getBooking().getId());

        if (remainingPending == 0) {
            // All pending repairs resolved -> return to SERVICE_IN_PROGRESS
            workflow.setStatus(WorkflowStatus.SERVICE_IN_PROGRESS);
            workflowRepository.save(workflow);

            wsPublisher.publishWorkflowUpdate(
                    repair.getBooking().getId(),
                    WorkflowStatus.SERVICE_IN_PROGRESS,
                    "Additional repair approved. Resuming service in progress."
            );
        } else {
            // Other repairs still pending -> remain AWAITING_APPROVAL
            workflow.setStatus(WorkflowStatus.AWAITING_APPROVAL);
            workflowRepository.save(workflow);
        }

        // Notify customer confirmation
        NotificationResponse custNotif = notificationService.createNotification(
                customer,
                "Additional Repair Approved",
                "You have approved: " + repair.getDescription() + " (₹" + repair.getEstimatedAmount() + ").",
                NotificationType.REPAIR_APPROVED,
                repair.getBooking().getId()
        );
        wsPublisher.publishNotification(customer.getId(), custNotif);

        // Notify Service Center staff
        try {
            List<User> scUsers = userRepository.findByRole(Role.SERVICE_CENTER);
            for (User scUser : scUsers) {
                NotificationResponse scNotif = notificationService.createNotification(
                        scUser,
                        "Additional Repair Approved",
                        "Customer " + customer.getName() + " approved: " + repair.getDescription() + " (₹" + repair.getEstimatedAmount() + ") for booking #" + repair.getBooking().getId() + ".",
                        NotificationType.REPAIR_APPROVED,
                        repair.getBooking().getId()
                );
                wsPublisher.publishNotification(scUser.getId(), scNotif);
            }
        } catch (Exception e) {
            // Ignore failure in notifying secondary staff
        }

        return AdditionalRepairResponse.fromEntity(updatedRepair);
    }

    /**
     * Customer rejects an additional repair request.
     * If all pending repairs for the booking are resolved:
     * - If at least one repair was approved -> transitions to SERVICE_IN_PROGRESS.
     * - If all repairs were rejected -> transitions to QUALITY_CHECK.
     */
    @Transactional
    public AdditionalRepairResponse rejectAdditionalRepair(Long repairId, String customerEmail) {
        AdditionalRepair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new ResourceNotFoundException("Additional repair request not found with id: " + repairId));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));

        if (!repair.getBooking().getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to authorize repairs for this vehicle.");
        }

        if (repair.getStatus() != RepairStatus.PENDING) {
            throw new InvalidRepairStateException("This repair request has already been " + repair.getStatus().name() + " and cannot be modified.");
        }

        repair.setStatus(RepairStatus.REJECTED);
        repair.setRespondedAt(LocalDateTime.now());
        AdditionalRepair updatedRepair = repairRepository.save(repair);

        // Check remaining pending repairs for this booking
        long remainingPending = repairRepository.countByBookingIdAndStatus(repair.getBooking().getId(), RepairStatus.PENDING);
        ServiceWorkflow workflow = getWorkflowOrThrow(repair.getBooking().getId());

        if (remainingPending == 0) {
            long approvedCount = repairRepository.countByBookingIdAndStatus(repair.getBooking().getId(), RepairStatus.APPROVED);
            if (approvedCount > 0) {
                workflow.setStatus(WorkflowStatus.SERVICE_IN_PROGRESS);
            } else {
                workflow.setStatus(WorkflowStatus.QUALITY_CHECK);
            }
            workflowRepository.save(workflow);

            wsPublisher.publishWorkflowUpdate(
                    repair.getBooking().getId(),
                    workflow.getStatus(),
                    "Additional repair decision recorded. Workflow advanced to " + workflow.getStatus().getDisplayName() + "."
            );
        } else {
            workflow.setStatus(WorkflowStatus.AWAITING_APPROVAL);
            workflowRepository.save(workflow);
        }

        // Notify customer confirmation
        NotificationResponse custNotif = notificationService.createNotification(
                customer,
                "Additional Repair Rejected",
                "You have declined: " + repair.getDescription() + ".",
                NotificationType.REPAIR_REJECTED,
                repair.getBooking().getId()
        );
        wsPublisher.publishNotification(customer.getId(), custNotif);

        // Notify Service Center staff
        try {
            List<User> scUsers = userRepository.findByRole(Role.SERVICE_CENTER);
            for (User scUser : scUsers) {
                NotificationResponse scNotif = notificationService.createNotification(
                        scUser,
                        "Additional Repair Rejected",
                        "Customer " + customer.getName() + " declined: " + repair.getDescription() + " for booking #" + repair.getBooking().getId() + ".",
                        NotificationType.REPAIR_REJECTED,
                        repair.getBooking().getId()
                );
                wsPublisher.publishNotification(scUser.getId(), scNotif);
            }
        } catch (Exception e) {
            // Ignore failure in notifying secondary staff
        }

        return AdditionalRepairResponse.fromEntity(updatedRepair);
    }

    /**
     * Retrieves workflow details for a booking with data isolation check.
     */
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowForBooking(Long bookingId, String userEmail) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service booking not found with id: " + bookingId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // If caller is CUSTOMER, ensure ownership
        if (user.getRole() == Role.CUSTOMER && !booking.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view workflow details for this booking.");
        }

        ServiceWorkflow workflow = workflowRepository.findByBookingId(bookingId).orElse(null);
        List<AdditionalRepair> repairs = repairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId);

        return WorkflowResponse.fromBookingAndWorkflow(booking, workflow, repairs);
    }

    /**
     * Lists all service bookings with their workflow details for the Service Center operations queue.
     */
    @Transactional(readOnly = true)
    public List<WorkflowResponse> getAllServiceCenterWorkflows() {
        return bookingRepository.findAllByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(b -> {
                    ServiceWorkflow workflow = workflowRepository.findByBookingId(b.getId()).orElse(null);
                    List<AdditionalRepair> repairs = repairRepository.findByBookingIdOrderByRequestedAtDesc(b.getId());
                    return WorkflowResponse.fromBookingAndWorkflow(b, workflow, repairs);
                })
                .collect(Collectors.toList());
    }

    /**
     * Lists all additional repairs for a specific booking.
     */
    @Transactional(readOnly = true)
    public List<AdditionalRepairResponse> getRepairsForBooking(Long bookingId, String userEmail) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service booking not found with id: " + bookingId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        if (user.getRole() == Role.CUSTOMER && !booking.getCustomer().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to view repairs for this booking.");
        }

        return repairRepository.findByBookingIdOrderByRequestedAtDesc(bookingId)
                .stream()
                .map(AdditionalRepairResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private ServiceWorkflow getWorkflowOrThrow(Long bookingId) {
        ServiceBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidWorkflowTransitionException("Cannot advance or modify workflow for a cancelled booking.");
        }

        return workflowRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Service workflow has not been started for booking id: " + bookingId));
    }

    private void notifyAndBroadcast(User customer, String title, String message, NotificationType type,
                                    Long bookingId, WorkflowStatus status) {
        NotificationResponse notification = notificationService.createNotification(
                customer,
                title,
                message,
                type,
                bookingId
        );
        wsPublisher.publishNotification(customer.getId(), notification);
        wsPublisher.publishWorkflowUpdate(bookingId, status, message);
    }
}
