package com.example.carservice.service;

import com.example.carservice.dto.VehicleRequest;
import com.example.carservice.dto.VehicleResponse;
import com.example.carservice.entity.User;
import com.example.carservice.entity.Vehicle;
import com.example.carservice.exception.DuplicateRegistrationNumberException;
import com.example.carservice.exception.ResourceNotFoundException;
import com.example.carservice.repository.UserRepository;
import com.example.carservice.repository.VehicleRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * VehicleService
 *
 * Implements business logic and ownership security rules for customer vehicle management.
 */
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserRepository userRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Registers a new vehicle assigned directly to the authenticated customer.
     */
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request, String userEmail) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        String normalizedReg = request.getRegistrationNumber().trim().toUpperCase();

        if (vehicleRepository.existsByRegistrationNumber(normalizedReg)) {
            throw new DuplicateRegistrationNumberException(
                    "A vehicle with registration number '" + normalizedReg + "' already exists."
            );
        }

        Vehicle vehicle = new Vehicle(
                owner,
                normalizedReg,
                request.getMake(),
                request.getModel(),
                request.getManufacturingYear(),
                request.getFuelType(),
                request.getTransmission(),
                request.getCurrentMileage()
        );

        Vehicle saved = vehicleRepository.save(vehicle);
        return new VehicleResponse(saved);
    }

    /**
     * Returns all vehicles owned by the authenticated customer.
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(String userEmail) {
        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        return vehicleRepository.findByOwnerId(owner.getId())
                .stream()
                .map(VehicleResponse::new)
                .toList();
    }

    /**
     * Returns vehicle details if owned by the authenticated customer.
     */
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        // Enforce ownership: only vehicle owner can access
        if (!vehicle.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You do not own this vehicle.");
        }

        return new VehicleResponse(vehicle);
    }

    /**
     * Updates vehicle details after verifying ownership and license plate uniqueness.
     */
    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleRequest request, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        // Enforce ownership
        if (!vehicle.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You cannot modify a vehicle you do not own.");
        }

        String normalizedReg = request.getRegistrationNumber().trim().toUpperCase();

        // Check if registration number is being changed to an existing plate belonging to another car
        Optional<Vehicle> existing = vehicleRepository.findByRegistrationNumber(normalizedReg);
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new DuplicateRegistrationNumberException(
                    "A vehicle with registration number '" + normalizedReg + "' already exists."
            );
        }

        vehicle.setRegistrationNumber(normalizedReg);
        vehicle.setMake(request.getMake().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setManufacturingYear(request.getManufacturingYear());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setTransmission(request.getTransmission());
        vehicle.setCurrentMileage(request.getCurrentMileage() != null ? request.getCurrentMileage() : 0);

        Vehicle updated = vehicleRepository.save(vehicle);
        return new VehicleResponse(updated);
    }

    /**
     * Deletes a vehicle after verifying customer ownership.
     */
    @Transactional
    public void deleteVehicle(Long id, String userEmail) {
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        // Enforce ownership
        if (!vehicle.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied: You cannot delete a vehicle you do not own.");
        }

        vehicleRepository.delete(vehicle);
    }
}
