package com.example.carservice.repository;

import com.example.carservice.entity.AdditionalRepair;
import com.example.carservice.entity.RepairStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AdditionalRepairRepository
 *
 * Repository for additional repair requests.
 */
@Repository
public interface AdditionalRepairRepository extends JpaRepository<AdditionalRepair, Long> {

    List<AdditionalRepair> findByBookingIdOrderByRequestedAtDesc(Long bookingId);

    List<AdditionalRepair> findByBookingIdAndStatus(Long bookingId, RepairStatus status);

    long countByBookingIdAndStatus(Long bookingId, RepairStatus status);
}
