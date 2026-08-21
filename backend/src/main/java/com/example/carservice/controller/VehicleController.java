package com.example.carservice.controller;

import com.example.carservice.dto.ApiResponse;
import com.example.carservice.dto.VehicleRequest;
import com.example.carservice.dto.VehicleResponse;
import com.example.carservice.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * VehicleController
 *
 * Exposes REST endpoints for Customer Vehicle Management.
 * All operations require authentication and enforce customer vehicle ownership.
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * Customer Vehicle Creation
     * POST /api/vehicles
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<VehicleResponse> createVehicle(
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication
    ) {
        VehicleResponse response = vehicleService.createVehicle(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get All Vehicles for Authenticated Customer
     * GET /api/vehicles
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<VehicleResponse>> getMyVehicles(Authentication authentication) {
        List<VehicleResponse> vehicles = vehicleService.getMyVehicles(authentication.getName());
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Get Specific Vehicle Details by ID (Owner Only)
     * GET /api/vehicles/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<VehicleResponse> getVehicleById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        VehicleResponse vehicle = vehicleService.getVehicleById(id, authentication.getName());
        return ResponseEntity.ok(vehicle);
    }

    /**
     * Update Vehicle Details (Owner Only)
     * PUT /api/vehicles/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request,
            Authentication authentication
    ) {
        VehicleResponse updated = vehicleService.updateVehicle(id, request, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete Vehicle (Owner Only)
     * DELETE /api/vehicles/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse> deleteVehicle(
            @PathVariable Long id,
            Authentication authentication
    ) {
        vehicleService.deleteVehicle(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Vehicle deleted successfully."));
    }
}
