package com.example.carservice.repository;

import com.example.carservice.entity.ServiceWorkflow;
import com.example.carservice.entity.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ServiceWorkflowRepository
 *
 * Repository for workshop service workflows.
 */
@Repository
public interface ServiceWorkflowRepository extends JpaRepository<ServiceWorkflow, Long> {

    Optional<ServiceWorkflow> findByBookingId(Long bookingId);

    List<ServiceWorkflow> findByStatus(WorkflowStatus status);
}
