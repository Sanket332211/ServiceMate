package com.example.carservice.repository;

import com.example.carservice.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ServiceItemRepository
 *
 * Repository for itemized service and parts components.
 */
@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    List<ServiceItem> findByServiceRecordId(Long serviceRecordId);
}
