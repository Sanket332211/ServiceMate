package com.example.carservice.controller;

import com.example.carservice.dto.*;
import com.example.carservice.service.ServiceHistoryService;
import com.example.carservice.service.ServiceWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * ServiceCenterOperationsController
 *
 * REST Controller for workshop bay operations, vehicle intake, workflow milestone transitions,
 * additional repair requests, and service completion details entry. Restricted to users with the SERVICE_CENTER role.
 */
@RestController
@RequestMapping("/api/service-center/bookings")
@PreAuthorize("hasRole('SERVICE_CENTER')")
public class ServiceCenterOperationsController {

    private final ServiceWorkflowService workflowService;
    private final ServiceHistoryService serviceHistoryService;

    public ServiceCenterOperationsController(ServiceWorkflowService workflowService,
                                             ServiceHistoryService serviceHistoryService) {
        this.workflowService = workflowService;
        this.serviceHistoryService = serviceHistoryService;
    }

    /**
     * GET /api/service-center/bookings
     * Returns all active workshop bookings with their current service workflow milestones.
     */
    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getAllBookings() {
        List<WorkflowResponse> list = workflowService.getAllServiceCenterWorkflows();
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/service-center/bookings/{bookingId}
     * Returns details of a specific service booking and its workflow milestones.
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<WorkflowResponse> getBookingWorkflow(@PathVariable Long bookingId, Principal principal) {
        WorkflowResponse response = workflowService.getWorkflowForBooking(bookingId, principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/service-center/bookings/{bookingId}/receive
     * CONFIRMED -> CAR_RECEIVED
     */
    @PostMapping("/{bookingId}/receive")
    public ResponseEntity<WorkflowResponse> receiveVehicle(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        WorkflowResponse response = workflowService.receiveVehicle(bookingId, notes);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/service-center/bookings/{bookingId}/start-inspection
     * CAR_RECEIVED -> INSPECTION
     */
    @PostMapping("/{bookingId}/start-inspection")
    public ResponseEntity<WorkflowResponse> startInspection(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        WorkflowResponse response = workflowService.startInspection(bookingId, notes);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/service-center/bookings/{bookingId}/start-service
     * INSPECTION -> SERVICE_IN_PROGRESS
     */
    @PostMapping("/{bookingId}/start-service")
    public ResponseEntity<WorkflowResponse> startService(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        WorkflowResponse response = workflowService.startService(bookingId, notes);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/service-center/bookings/{bookingId}/start-quality-check
     * SERVICE_IN_PROGRESS -> QUALITY_CHECK
     */
    @PostMapping("/{bookingId}/start-quality-check")
    public ResponseEntity<WorkflowResponse> startQualityCheck(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        WorkflowResponse response = workflowService.startQualityCheck(bookingId, notes);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/service-center/bookings/{bookingId}/mark-ready
     * QUALITY_CHECK -> READY_FOR_DELIVERY
     */
    @PostMapping("/{bookingId}/mark-ready")
    public ResponseEntity<WorkflowResponse> markReady(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        WorkflowResponse response = workflowService.markReadyForDelivery(bookingId, notes);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/service-center/bookings/{bookingId}/complete
     * READY_FOR_DELIVERY -> COMPLETED
     */
    @PostMapping("/{bookingId}/complete")
    public ResponseEntity<WorkflowResponse> completeService(
            @PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        WorkflowResponse response = workflowService.completeService(bookingId, notes);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/service-center/bookings/{bookingId}/repairs
     * Creates an additional repair estimate requiring customer approval.
     */
    @PostMapping("/{bookingId}/repairs")
    public ResponseEntity<AdditionalRepairResponse> createAdditionalRepair(
            @PathVariable Long bookingId,
            @Valid @RequestBody AdditionalRepairRequest request) {
        AdditionalRepairResponse response = workflowService.createAdditionalRepair(bookingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/service-center/bookings/{bookingId}/service-record
     * Enters or updates itemized service completion details during QUALITY_CHECK.
     */
    @PostMapping("/{bookingId}/service-record")
    public ResponseEntity<ServiceRecordResponse> saveServiceCompletionDetails(
            @PathVariable Long bookingId,
            @Valid @RequestBody ServiceCompletionRequest request) {
        ServiceRecordResponse response = serviceHistoryService.saveOrUpdateServiceCompletionDetails(bookingId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/service-center/bookings/{bookingId}/service-record
     * Retrieves service completion details for a booking.
     */
    @GetMapping("/{bookingId}/service-record")
    public ResponseEntity<ServiceRecordResponse> getServiceCompletionDetails(
            @PathVariable Long bookingId,
            Principal principal) {
        ServiceRecordResponse response = serviceHistoryService.getServiceRecordForBooking(bookingId, principal.getName());
        return ResponseEntity.ok(response);
    }
}
