package com.example.carservice.controller;

import com.example.carservice.dto.AiServiceAdvisorRequest;
import com.example.carservice.dto.AiServiceAdvisorResponse;
import com.example.carservice.dto.AiServiceSummaryResponse;
import com.example.carservice.service.AiAdvisorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * AiController
 *
 * REST Controller for AI Service Advisor and AI Service Summary features.
 * All requests are strictly authenticated and protected by Spring Security role checks.
 */
@RestController
@RequestMapping("/api")
public class AiController {

    private final AiAdvisorService aiAdvisorService;

    public AiController(AiAdvisorService aiAdvisorService) {
        this.aiAdvisorService = aiAdvisorService;
    }

    /**
     * POST /api/ai/service-advisor
     * Returns AI-assisted service recommendations for a customer's vehicle problem.
     */
    @PostMapping("/ai/service-advisor")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AiServiceAdvisorResponse> getServiceAdvice(
            @Valid @RequestBody AiServiceAdvisorRequest request,
            Principal principal) {
        AiServiceAdvisorResponse response = aiAdvisorService.getServiceAdvice(
                request.getVehicleId(),
                request.getProblemDescription(),
                principal.getName()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/service-records/{recordId}/ai-summary
     * Generates a customer-friendly AI summary of a finalized service record.
     */
    @GetMapping("/service-records/{recordId}/ai-summary")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SERVICE_CENTER')")
    public ResponseEntity<AiServiceSummaryResponse> getServiceSummary(
            @PathVariable Long recordId,
            Principal principal) {
        AiServiceSummaryResponse response = aiAdvisorService.getServiceSummary(recordId, principal.getName());
        return ResponseEntity.ok(response);
    }
}
