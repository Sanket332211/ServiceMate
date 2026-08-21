package com.example.carservice.repository;

import com.example.carservice.entity.InspectionFinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * InspectionFindingRepository
 *
 * Repository for inspection observations.
 */
@Repository
public interface InspectionFindingRepository extends JpaRepository<InspectionFinding, Long> {

    List<InspectionFinding> findByServiceRecordId(Long serviceRecordId);
}
