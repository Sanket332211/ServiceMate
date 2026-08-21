package com.example.carservice.service;

import com.example.carservice.dto.BookingRequest;
import com.example.carservice.dto.BookingResponse;
import com.example.carservice.dto.NotificationResponse;
import com.example.carservice.dto.SlotAvailabilityResponse;
import com.example.carservice.entity.*;
import com.example.carservice.exception.DuplicateBookingException;
import com.example.carservice.exception.InvalidBookingDateException;
import com.example.carservice.exception.ResourceNotFoundException;
import com.example.carservice.exception.SlotFullException;
import com.example.carservice.repository.ServiceBookingRepository;
import com.example.carservice.repository.UserRepository;
import com.example.carservice.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ServiceBookingService
 *
 * Implements business rules for capacity-controlled service bookings:
 * 1. Strict capacity limit: MAX 2 CONFIRMED bookings per time slot.
 * 2. Concurrency-safe booking allocation.
 * 3. 7-day booking window and past date/time validation.
 * 4. Customer vehicle ownership security.
 * 5. Duplicate vehicle booking prevention.
 * 6. Server-side price calculation (service estimate + pickup/drop charge).
 * 7. Customer self-service cancellation (releasing slot capacity).
 * 8. Automated persistent in-app notifications on booking confirmation and cancellation.
 */
@Service
public class ServiceBookingService {

    private static final Logger log = LoggerFactory.getLogger(ServiceBookingService.class);

    public static final int MAX_BOOKINGS_PER_SLOT = 2;
    public static final int PICKUP_DROP_FLAT_FEE = 300;
    public static final int MAX_BOOKING_WINDOW_DAYS = 7;

    private final ServiceBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final NotificationService notificationService;
    private final WorkflowWebSocketPublisher wsPublisher;

    public ServiceBookingService(ServiceBookingRepository bookingRepository,
                                 UserRepository userRepository,
                                 VehicleRepository vehicleRepository,
                                 NotificationService notificationService,
                                 WorkflowWebSocketPublisher wsPublisher) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.notificationService = notificationService;
        this.wsPublisher = wsPublisher;
    }

    /**
     * Creates a new capacity-controlled service booking for the authenticated customer.
     * Uses interned slot lock monitor and database transactional boundaries to prevent race conditions.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request, String userEmail) {
        User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));

        // Enforce vehicle ownership security
        if (!vehicle.getOwner().getId().equals(customer.getId())) {
            throw new AccessDeniedException("This vehicle does not belong to your account.");
        }

        // Validate booking date & window
        validateBookingDateAndTime(request.getBookingDate(), request.getTimeSlot());

        // Lock on date+slot key to ensure atomic capacity validation in concurrent requests
        String slotLockKey = (request.getBookingDate().toString() + ":" + request.getTimeSlot().name()).intern();
        synchronized (slotLockKey) {
            // Check for duplicate vehicle booking on same slot
            boolean isVehicleAlreadyBooked = bookingRepository.existsByVehicleIdAndBookingDateAndTimeSlotAndStatus(
                    vehicle.getId(),
                    request.getBookingDate(),
                    request.getTimeSlot(),
                    BookingStatus.CONFIRMED
            );
            if (isVehicleAlreadyBooked) {
                throw new DuplicateBookingException("This vehicle already has a confirmed booking for the selected time slot.");
            }

            // Check active capacity
            long activeBookings = bookingRepository.countByBookingDateAndTimeSlotAndStatus(
                    request.getBookingDate(),
                    request.getTimeSlot(),
                    BookingStatus.CONFIRMED
            );

            if (activeBookings >= MAX_BOOKINGS_PER_SLOT) {
                throw new SlotFullException("This time slot is full. Maximum " + MAX_BOOKINGS_PER_SLOT + " vehicles allowed per slot.");
            }

            // Resolve selected service packages (support single or multiple)
            List<ServiceType> selectedTypes = request.getResolvedServiceTypes();
            if (selectedTypes.isEmpty()) {
                throw new IllegalArgumentException("At least one service package must be selected.");
            }

            // Calculate official estimated amounts on backend
            int estimatedServiceAmount = selectedTypes.stream().mapToInt(ServiceType::getBasePrice).sum();
            int pickupDropCharge = request.isPickupDropRequired() ? PICKUP_DROP_FLAT_FEE : 0;
            int estimatedTotalAmount = estimatedServiceAmount + pickupDropCharge;

            ServiceType primaryType = selectedTypes.get(0);
            String summary = selectedTypes.stream().map(ServiceType::getDisplayName).collect(Collectors.joining(", "));

            ServiceBooking booking = new ServiceBooking(
                    customer,
                    vehicle,
                    primaryType,
                    summary,
                    request.getBookingDate(),
                    request.getTimeSlot(),
                    BookingStatus.CONFIRMED,
                    request.isPickupDropRequired(),
                    pickupDropCharge,
                    estimatedServiceAmount,
                    estimatedTotalAmount
            );

            ServiceBooking saved = bookingRepository.save(booking);

            // Generate persistent in-app notification & broadcast via WebSocket
            try {
                String vehicleDesc = vehicle.getMake() + " " + vehicle.getModel() + " (" + vehicle.getRegistrationNumber() + ")";
                NotificationResponse notif = notificationService.createNotification(
                        customer,
                        "Booking Confirmed",
                        "Your service booking for " + vehicleDesc + " has been confirmed for " + saved.getBookingDate() + " (" + saved.getTimeSlot().getLabel() + ").",
                        NotificationType.BOOKING_CONFIRMED,
                        saved.getId()
                );
                if (wsPublisher != null) {
                    wsPublisher.publishNotification(customer.getId(), notif);
                }
            } catch (Exception e) {
                log.warn("Failed to generate or publish booking confirmation notification: {}", e.getMessage());
            }

            return BookingResponse.fromEntity(saved);
        }
    }

    /**
     * Returns all bookings belonging to the authenticated customer.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String userEmail) {
        User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));

        return bookingRepository.findByCustomerIdOrderByCreatedAtDescIdDesc(customer.getId())
                .stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Returns details of a specific booking owned by the authenticated customer.
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, String userEmail) {
        User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));

        ServiceBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service booking not found with id: " + id));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to access this booking.");
        }

        return BookingResponse.fromEntity(booking);
    }

    /**
     * Cancels an existing CONFIRMED booking owned by the customer, immediately releasing slot capacity.
     */
    @Transactional
    public BookingResponse cancelBooking(Long id, String userEmail) {
        User customer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found."));

        ServiceBooking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service booking not found with id: " + id));

        if (!booking.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to cancel this booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new DuplicateBookingException("This booking has already been cancelled.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        ServiceBooking updated = bookingRepository.save(booking);

        // Generate persistent in-app notification & broadcast via WebSocket
        try {
            String vehicleDesc = booking.getVehicle().getMake() + " " + booking.getVehicle().getModel() + " (" + booking.getVehicle().getRegistrationNumber() + ")";
            NotificationResponse notif = notificationService.createNotification(
                    customer,
                    "Booking Cancelled",
                    "Your service booking for " + vehicleDesc + " on " + booking.getBookingDate() + " has been cancelled.",
                    NotificationType.BOOKING_CANCELLED,
                    booking.getId()
            );
            if (wsPublisher != null) {
                wsPublisher.publishNotification(customer.getId(), notif);
            }
        } catch (Exception e) {
            log.warn("Failed to generate or publish booking cancellation notification: {}", e.getMessage());
        }

        return BookingResponse.fromEntity(updated);
    }

    /**
     * Checks real-time capacity and availability for all 4 fixed time slots on a requested date.
     */
    @Transactional(readOnly = true)
    public List<SlotAvailabilityResponse> getAvailabilityForDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        boolean isDateInRange = !date.isBefore(today) && !date.isAfter(today.plusDays(MAX_BOOKING_WINDOW_DAYS));
        LocalTime now = LocalTime.now();

        List<SlotAvailabilityResponse> availabilityList = new ArrayList<>();

        for (TimeSlot slot : TimeSlot.values()) {
            long bookedCount = bookingRepository.countByBookingDateAndTimeSlotAndStatus(
                    date,
                    slot,
                    BookingStatus.CONFIRMED
            );

            int remaining = Math.max(0, MAX_BOOKINGS_PER_SLOT - (int) bookedCount);
            boolean isPastToday = date.isEqual(today) && now.isAfter(slot.getStartTime());
            boolean available = isDateInRange && !isPastToday && remaining > 0;

            availabilityList.add(new SlotAvailabilityResponse(
                    slot,
                    slot.getLabel(),
                    MAX_BOOKINGS_PER_SLOT,
                    (int) bookedCount,
                    remaining,
                    available
            ));
        }

        return availabilityList;
    }

    /**
     * Validates date is within allowed 7-day window and not elapsed for today.
     */
    private void validateBookingDateAndTime(LocalDate date, TimeSlot timeSlot) {
        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) {
            throw new InvalidBookingDateException("Cannot book a service appointment for a past date.");
        }

        if (date.isAfter(today.plusDays(MAX_BOOKING_WINDOW_DAYS))) {
            throw new InvalidBookingDateException("Bookings can only be scheduled up to " + MAX_BOOKING_WINDOW_DAYS + " days in advance.");
        }

        if (date.isEqual(today) && LocalTime.now().isAfter(timeSlot.getStartTime())) {
            throw new InvalidBookingDateException("The selected time slot (" + timeSlot.getLabel() + ") has already passed for today.");
        }
    }
}
