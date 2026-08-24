package com.example.carservice.controller;

import com.example.carservice.dto.BookingRequest;
import com.example.carservice.dto.BookingResponse;
import com.example.carservice.dto.SlotAvailabilityResponse;
import com.example.carservice.service.ServiceBookingService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * ServiceBookingController
 *
 * REST Controller for managing customer service bookings and checking slot capacity.
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(originPatterns = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.PATCH}, allowCredentials = "true")
public class ServiceBookingController {

    private final ServiceBookingService bookingService;

    public ServiceBookingController(ServiceBookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST /api/bookings
     * Creates a new capacity-controlled service booking for the authenticated customer.
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request, Principal principal) {
        BookingResponse response = bookingService.createBooking(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/bookings/my
     * Retrieves all bookings for the authenticated customer.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Principal principal) {
        List<BookingResponse> bookings = bookingService.getMyBookings(principal.getName());
        return ResponseEntity.ok(bookings);
    }

    /**
     * GET /api/bookings/{id}
     * Retrieves a single booking by ID (must belong to authenticated customer).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id, Principal principal) {
        BookingResponse booking = bookingService.getBookingById(id, principal.getName());
        return ResponseEntity.ok(booking);
    }

    /**
     * PATCH /api/bookings/{id}/cancel
     * Cancels a customer's booking and immediately frees up slot capacity.
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable Long id, Principal principal) {
        BookingResponse cancelled = bookingService.cancelBooking(id, principal.getName());
        return ResponseEntity.ok(cancelled);
    }

    /**
     * GET /api/bookings/availability?date=YYYY-MM-DD
     * Checks capacity and availability for all 4 fixed time slots on a requested date.
     */
    @GetMapping("/availability")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<SlotAvailabilityResponse>> getAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<SlotAvailabilityResponse> availability = bookingService.getAvailabilityForDate(date);
        return ResponseEntity.ok(availability);
    }
}
