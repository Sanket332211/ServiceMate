package com.example.carservice.repository;

import com.example.carservice.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * VehicleRepository
 *
 * Provides database operations for the 'vehicles' table using Spring Data JPA.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOwnerId(Long ownerId);

    List<Vehicle> findByOwnerEmail(String email);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<Vehicle> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<Vehicle> findByIdAndOwnerEmail(Long id, String email);
}
