package com.example.carservice.controller;

import com.example.carservice.dto.AdditionalRepairResponse;
import com.example.carservice.dto.WorkflowResponse;
import com.example.carservice.service.ServiceWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * CustomerWorkflowController
 *
 * REST Controller for customers to view live service milestones and authorize/decline additional repair requests.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH}, allowCredentials = "true")
public class CustomerWorkflowController {

    private final ServiceWorkflowService workflowService;

    public CustomerWorkflowController(ServiceWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * GET /api/bookings/{bookingId}/workflow
     * Retrieves the service workflow status and milestones for a customer's booking.
     */
    @GetMapping("/bookings/{bookingId}/workflow")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_CENTER')")
    public ResponseEntity<WorkflowResponse> getBookingWorkflow(
            @PathVariable Long bookingId,
            Principal principal) {
        WorkflowResponse response = workflowService.getWorkflowForBooking(bookingId, principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/bookings/{bookingId}/repairs
     * Lists all additional repairs requested for a booking.
     */
    @GetMapping("/bookings/{bookingId}/repairs")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_CENTER')")
    public ResponseEntity<List<AdditionalRepairResponse>> getRepairs(
            @PathVariable Long bookingId,
            Principal principal) {
        List<AdditionalRepairResponse> list = workflowService.getRepairsForBooking(bookingId, principal.getName());
        return ResponseEntity.ok(list);
    }

    /**
     * POST /api/repairs/{repairId}/approve
     * Customer authorizes an additional repair finding.
     */
    @PostMapping("/repairs/{repairId}/approve")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AdditionalRepairResponse> approveRepair(
            @PathVariable Long repairId,
            Principal principal) {
        AdditionalRepairResponse response = workflowService.approveAdditionalRepair(repairId, principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/repairs/{repairId}/reject
     * Customer declines an additional repair finding.
     */
    @PostMapping("/repairs/{repairId}/reject")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AdditionalRepairResponse> rejectRepair(
            @PathVariable Long repairId,
            Principal principal) {
        AdditionalRepairResponse response = workflowService.rejectAdditionalRepair(repairId, principal.getName());
        return ResponseEntity.ok(response);
    }
}
