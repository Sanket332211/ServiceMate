package com.example.carservice.controller;

import com.example.carservice.dto.ServiceRecordResponse;
import com.example.carservice.dto.VehicleServiceHistoryResponse;
import com.example.carservice.service.ServiceHistoryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * ServiceHistoryController
 *
 * REST Controller for accessing portable vehicle service records and downloading verified OpenPDF reports.
 */
@RestController
@RequestMapping("/api")
public class ServiceHistoryController {

    private final ServiceHistoryService serviceHistoryService;

    public ServiceHistoryController(ServiceHistoryService serviceHistoryService) {
        this.serviceHistoryService = serviceHistoryService;
    }

    /**
     * GET /api/vehicles/{vehicleId}/service-history
     * Retrieves the complete chronological service history dossier for a vehicle.
     */
    @GetMapping("/vehicles/{vehicleId}/service-history")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_CENTER')")
    public ResponseEntity<VehicleServiceHistoryResponse> getVehicleServiceHistory(
            @PathVariable Long vehicleId,
            Principal principal) {
        VehicleServiceHistoryResponse response = serviceHistoryService.getVehicleServiceHistory(vehicleId, principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/vehicles/{vehicleId}/service-history/pdf
     * Generates and downloads the complete multi-visit vehicle service history PDF.
     */
    @GetMapping("/vehicles/{vehicleId}/service-history/pdf")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_CENTER')")
    public ResponseEntity<byte[]> downloadVehicleHistoryPdf(
            @PathVariable Long vehicleId,
            Principal principal) {
        byte[] pdfBytes = serviceHistoryService.generateVehicleHistoryPdf(vehicleId, principal.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ServiceMate_Vehicle_History_" + vehicleId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * GET /api/service-records/{recordId}
     * Retrieves an individual finalized service record.
     */
    @GetMapping("/service-records/{recordId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_CENTER')")
    public ResponseEntity<ServiceRecordResponse> getSingleServiceRecord(
            @PathVariable Long recordId,
            Principal principal) {
        ServiceRecordResponse response = serviceHistoryService.getSingleServiceRecord(recordId, principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/service-records/{recordId}/pdf
     * Generates and downloads an individual service visit PDF report.
     */
    @GetMapping("/service-records/{recordId}/pdf")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_CENTER')")
    public ResponseEntity<byte[]> downloadSingleServicePdf(
            @PathVariable Long recordId,
            Principal principal) {
        byte[] pdfBytes = serviceHistoryService.generateSingleServicePdf(recordId, principal.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ServiceMate_Service_Record_" + recordId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
