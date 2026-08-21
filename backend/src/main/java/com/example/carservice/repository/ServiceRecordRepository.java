package com.example.carservice.repository;

import com.example.carservice.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ServiceRecordRepository
 *
 * Repository for historical vehicle service records and dossiers.
 */
@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    Optional<ServiceRecord> findByBookingId(Long bookingId);

    List<ServiceRecord> findByVehicleIdAndFinalizedAtIsNotNullOrderByServiceDateDescFinalizedAtDesc(Long vehicleId);

    List<ServiceRecord> findByVehicleIdOrderByServiceDateDesc(Long vehicleId);
}
