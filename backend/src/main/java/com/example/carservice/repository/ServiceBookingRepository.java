package com.example.carservice.repository;

import com.example.carservice.entity.BookingStatus;
import com.example.carservice.entity.ServiceBooking;
import com.example.carservice.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ServiceBookingRepository
 *
 * Spring Data JPA repository for service bookings.
 */
@Repository
public interface ServiceBookingRepository extends JpaRepository<ServiceBooking, Long> {

    /**
     * Finds all service bookings for a given customer ordered by creation time descending.
     */
    List<ServiceBooking> findByCustomerIdOrderByCreatedAtDescIdDesc(Long customerId);

    /**
     * Finds all service bookings ordered by creation time descending for workshop operations queue.
     */
    List<ServiceBooking> findAllByOrderByCreatedAtDescIdDesc();

    /**
     * Finds all service bookings for a given customer ordered by booking date and creation time descending.
     */
    List<ServiceBooking> findByCustomerIdOrderByBookingDateDescCreatedAtDesc(Long customerId);

    /**
     * Counts active capacity-consuming bookings for a specific date and time slot.
     * In Phase 4, only CONFIRMED bookings consume capacity.
     */
    long countByBookingDateAndTimeSlotAndStatus(LocalDate bookingDate, TimeSlot timeSlot, BookingStatus status);

    /**
     * Checks whether a specific vehicle is already booked for a given date, time slot, and status.
     */
    boolean existsByVehicleIdAndBookingDateAndTimeSlotAndStatus(Long vehicleId, LocalDate bookingDate, TimeSlot timeSlot, BookingStatus status);

    /**
     * Finds a booking by ID and customer ID to strictly enforce customer ownership.
     */
    Optional<ServiceBooking> findByIdAndCustomerId(Long id, Long customerId);
}
