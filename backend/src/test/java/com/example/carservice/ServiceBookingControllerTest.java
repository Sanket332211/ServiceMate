package com.example.carservice;

import com.example.carservice.dto.BookingRequest;
import com.example.carservice.entity.*;
import com.example.carservice.repository.*;
import com.example.carservice.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ServiceBookingControllerTest
 *
 * Automated integration tests for Phase 4: Capacity-Controlled Service Booking.
 * Verifies capacity limits (MAX 2), concurrency safety, pricing rules, cancellation, and security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ServiceBookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceBookingRepository bookingRepository;

    @Autowired
    private ServiceWorkflowRepository workflowRepository;

    @Autowired
    private AdditionalRepairRepository repairRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User customer1;
    private User customer2;
    private User customer3;
    private Vehicle vehicle1;
    private Vehicle vehicle2;
    private Vehicle vehicle3;
    private String token1;
    private String token2;
    private String token3;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        repairRepository.deleteAll();
        workflowRepository.deleteAll();
        bookingRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        // Customer 1
        customer1 = new User("Aarav Sharma", "aarav@example.com", "9876543210", passwordEncoder.encode("Password123!"), Role.CUSTOMER);
        customer1 = userRepository.save(customer1);
        token1 = jwtService.generateToken(customer1);

        vehicle1 = new Vehicle(customer1, "MH12AB1001", "Honda", "City", 2021, FuelType.PETROL, Transmission.AUTOMATIC, 25000);
        vehicle1 = vehicleRepository.save(vehicle1);

        // Customer 2
        customer2 = new User("Priya Patel", "priya@example.com", "9876543211", passwordEncoder.encode("Password123!"), Role.CUSTOMER);
        customer2 = userRepository.save(customer2);
        token2 = jwtService.generateToken(customer2);

        vehicle2 = new Vehicle(customer2, "MH12AB2002", "Hyundai", "i20", 2022, FuelType.PETROL, Transmission.MANUAL, 18000);
        vehicle2 = vehicleRepository.save(vehicle2);

        // Customer 3
        customer3 = new User("Vikram Singh", "vikram@example.com", "9876543212", passwordEncoder.encode("Password123!"), Role.CUSTOMER);
        customer3 = userRepository.save(customer3);
        token3 = jwtService.generateToken(customer3);

        vehicle3 = new Vehicle(customer3, "MH12AB3003", "Tata", "Nexon", 2023, FuelType.ELECTRIC, Transmission.AUTOMATIC, 9000);
        vehicle3 = vehicleRepository.save(vehicle3);
    }

    @Test
    @DisplayName("TEST 1: Customer successfully creates a service booking (Status: CONFIRMED)")
    void testSuccessfulBooking() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest(
                vehicle1.getId(),
                ServiceType.GENERAL_SERVICE,
                validDate,
                TimeSlot.MORNING_SLOT_1,
                false
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.serviceType").value("GENERAL_SERVICE"))
                .andExpect(jsonPath("$.timeSlot").value("MORNING_SLOT_1"))
                .andExpect(jsonPath("$.estimatedServiceAmount").value(1499))
                .andExpect(jsonPath("$.pickupDropCharge").value(0))
                .andExpect(jsonPath("$.estimatedTotalAmount").value(1499))
                .andExpect(jsonPath("$.vehicleRegistrationNumber").value("MH12AB1001"));

        assertEquals(1, bookingRepository.count());
    }

    @Test
    @DisplayName("TEST 2: Second customer successfully books the same time slot (Capacity reaches 2/2)")
    void testSecondBookingAllowed() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);

        // Booking 1 (Customer 1)
        BookingRequest req1 = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, validDate, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // Booking 2 (Customer 2)
        BookingRequest req2 = new BookingRequest(vehicle2.getId(), ServiceType.OIL_CHANGE, validDate, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        long count = bookingRepository.countByBookingDateAndTimeSlotAndStatus(validDate, TimeSlot.MORNING_SLOT_1, BookingStatus.CONFIRMED);
        assertEquals(2, count);
    }

    @Test
    @DisplayName("TEST 3: Third booking attempt on the same slot is rejected with 409 Conflict (Slot Full)")
    void testThirdBookingRejectedWhenSlotFull() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);

        // Fill slot to capacity (2/2)
        BookingRequest req1 = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, validDate, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1)));

        BookingRequest req2 = new BookingRequest(vehicle2.getId(), ServiceType.OIL_CHANGE, validDate, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2)));

        // Third booking attempt (Customer 3)
        BookingRequest req3 = new BookingRequest(vehicle3.getId(), ServiceType.BRAKE_SERVICE, validDate, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token3)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("This time slot is full")));

        assertEquals(2, bookingRepository.count());
    }

    @Test
    @DisplayName("TEST 4: Booking cancellation releases capacity allowing a new customer to book")
    void testCancellationReleasesCapacity() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);

        // Create 2 bookings (Slot Full)
        BookingRequest req1 = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, validDate, TimeSlot.MORNING_SLOT_1, false);
        MvcResult res1 = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1)))
                .andReturn();
        Long booking1Id = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        BookingRequest req2 = new BookingRequest(vehicle2.getId(), ServiceType.OIL_CHANGE, validDate, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2)));

        // Cancel Booking 1
        mockMvc.perform(patch("/api/bookings/" + booking1Id + "/cancel")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Now Customer 3 can successfully book the freed slot!
        BookingRequest req3 = new BookingRequest(vehicle3.getId(), ServiceType.BRAKE_SERVICE, validDate, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token3)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        long confirmedCount = bookingRepository.countByBookingDateAndTimeSlotAndStatus(validDate, TimeSlot.MORNING_SLOT_1, BookingStatus.CONFIRMED);
        assertEquals(2, confirmedCount);
    }

    @Test
    @DisplayName("TEST 5: Booking rejected with 403 Forbidden if vehicle belongs to another customer")
    void testVehicleOwnershipEnforcement() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);
        // Customer 1 tries to use Customer 2's vehicle (vehicle2)
        BookingRequest request = new BookingRequest(
                vehicle2.getId(),
                ServiceType.GENERAL_SERVICE,
                validDate,
                TimeSlot.MORNING_SLOT_1,
                false
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 6: Duplicate booking of same vehicle on same slot is rejected with 409 Conflict")
    void testDuplicateVehicleBookingSameSlot() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest(
                vehicle1.getId(),
                ServiceType.GENERAL_SERVICE,
                validDate,
                TimeSlot.MORNING_SLOT_1,
                false
        );

        // First booking
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate booking attempt
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already has a confirmed booking")));
    }

    @Test
    @DisplayName("TEST 7: Booking on a past date is rejected with 400 Bad Request")
    void testPastDateRejected() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        BookingRequest request = new BookingRequest(
                vehicle1.getId(),
                ServiceType.GENERAL_SERVICE,
                pastDate,
                TimeSlot.MORNING_SLOT_1,
                false
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("past date")));
    }

    @Test
    @DisplayName("TEST 8: Booking beyond the 7-day booking window is rejected with 400 Bad Request")
    void testBeyondBookingWindowRejected() throws Exception {
        LocalDate farFuture = LocalDate.now().plusDays(10);
        BookingRequest request = new BookingRequest(
                vehicle1.getId(),
                ServiceType.GENERAL_SERVICE,
                farFuture,
                TimeSlot.MORNING_SLOT_1,
                false
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("7 days in advance")));
    }

    @Test
    @DisplayName("TEST 9: Pickup and drop calculates additional ₹300 charge correctly")
    void testPickupDropCalculation() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest(
                vehicle1.getId(),
                ServiceType.GENERAL_SERVICE, // Base: 1499
                validDate,
                TimeSlot.MORNING_SLOT_1,
                true // Pickup/drop: +300
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pickupDropRequired").value(true))
                .andExpect(jsonPath("$.pickupDropCharge").value(300))
                .andExpect(jsonPath("$.estimatedServiceAmount").value(1499))
                .andExpect(jsonPath("$.estimatedTotalAmount").value(1799));
    }

    @Test
    @DisplayName("TEST 10: Customer cannot cancel another customer's booking (403 Forbidden)")
    void testCustomerCannotCancelOtherCustomerBooking() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);
        BookingRequest req = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, validDate, TimeSlot.MORNING_SLOT_1, false);

        MvcResult res = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andReturn();
        Long bookingId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Customer 2 tries to cancel Customer 1's booking
        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 11: Unauthenticated request to /api/bookings returns 401 Unauthorized")
    void testUnauthenticatedBookingRejected() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest(
                vehicle1.getId(),
                ServiceType.GENERAL_SERVICE,
                validDate,
                TimeSlot.MORNING_SLOT_1,
                false
        );

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TEST 12: Availability API returns 4 fixed slots with capacity and booked count")
    void testSlotAvailabilityApi() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(2);

        // Pre-book 1 slot
        BookingRequest req = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, validDate, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)));

        mockMvc.perform(get("/api/bookings/availability")
                        .param("date", validDate.toString())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].slot").value("MORNING_SLOT_1"))
                .andExpect(jsonPath("$[0].capacity").value(2))
                .andExpect(jsonPath("$[0].booked").value(1))
                .andExpect(jsonPath("$[0].remaining").value(1))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[1].slot").value("MORNING_SLOT_2"))
                .andExpect(jsonPath("$[1].capacity").value(2))
                .andExpect(jsonPath("$[1].booked").value(0))
                .andExpect(jsonPath("$[1].remaining").value(2))
                .andExpect(jsonPath("$[1].available").value(true));
    }

    @Test
    @DisplayName("TEST 13 (CONCURRENCY): Two simultaneous requests for the last slot results in exactly 1 success and 1 rejection")
    void testConcurrentBookingAttemptsRespectCapacity() throws Exception {
        LocalDate validDate = LocalDate.now().plusDays(3);

        // Pre-book 1 slot so only 1 spot remains (Current capacity = 1/2)
        BookingRequest preReq = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, validDate, TimeSlot.AFTERNOON_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(preReq)))
                .andExpect(status().isCreated());

        // Now, Customer 2 and Customer 3 will simultaneously attempt to book the single remaining spot
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Callable<Integer> taskCustomer2 = () -> {
            latch.await();
            BookingRequest req = new BookingRequest(vehicle2.getId(), ServiceType.OIL_CHANGE, validDate, TimeSlot.AFTERNOON_SLOT_1, false);
            MvcResult result = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token2)
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andReturn();
            return result.getResponse().getStatus();
        };

        Callable<Integer> taskCustomer3 = () -> {
            latch.await();
            BookingRequest req = new BookingRequest(vehicle3.getId(), ServiceType.BATTERY_SERVICE, validDate, TimeSlot.AFTERNOON_SLOT_1, false);
            MvcResult result = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token3)
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                    .andReturn();
            return result.getResponse().getStatus();
        };

        Future<Integer> future2 = executor.submit(taskCustomer2);
        Future<Integer> future3 = executor.submit(taskCustomer3);

        // Fire both requests simultaneously
        latch.countDown();

        int status2 = future2.get(5, TimeUnit.SECONDS);
        int status3 = future3.get(5, TimeUnit.SECONDS);

        if (status2 == 201) successCount.incrementAndGet();
        if (status2 == 409) conflictCount.incrementAndGet();

        if (status3 == 201) successCount.incrementAndGet();
        if (status3 == 409) conflictCount.incrementAndGet();

        executor.shutdown();

        // Exactly one request must succeed (201) and exactly one must fail with 409 Conflict
        assertEquals(1, successCount.get(), "Exactly one concurrent booking must succeed");
        assertEquals(1, conflictCount.get(), "Exactly one concurrent booking must be rejected with 409 Conflict");

        // The final active booking count for this slot in DB must remain exactly 2
        long finalConfirmed = bookingRepository.countByBookingDateAndTimeSlotAndStatus(validDate, TimeSlot.AFTERNOON_SLOT_1, BookingStatus.CONFIRMED);
        assertEquals(2, finalConfirmed, "Total confirmed bookings must not exceed capacity 2");
    }

    @Test
    @DisplayName("TEST 14: Customer Booking History returns most recently created booking first")
    void testCustomerBookingHistoryReturnsNewestFirst() throws Exception {
        LocalDate date1 = LocalDate.now().plusDays(5);
        LocalDate date2 = LocalDate.now().plusDays(2);
        LocalDate date3 = LocalDate.now().plusDays(3);

        // Create booking 1
        BookingRequest req1 = new BookingRequest(vehicle1.getId(), ServiceType.OIL_CHANGE, date1, TimeSlot.MORNING_SLOT_1, false);
        MvcResult res1 = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();
        long id1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        // Create booking 2
        BookingRequest req2 = new BookingRequest(vehicle1.getId(), ServiceType.AC_SERVICE, date2, TimeSlot.MORNING_SLOT_2, false);
        MvcResult res2 = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();
        long id2 = objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asLong();

        // Create booking 3
        BookingRequest req3 = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, date3, TimeSlot.AFTERNOON_SLOT_1, false);
        MvcResult res3 = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated())
                .andReturn();
        long id3 = objectMapper.readTree(res3.getResponse().getContentAsString()).get("id").asLong();

        // Fetch /api/bookings/my -> Must be ordered [id3, id2, id1]
        mockMvc.perform(get("/api/bookings/my").header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(id3))
                .andExpect(jsonPath("$[1].id").value(id2))
                .andExpect(jsonPath("$[2].id").value(id1));
    }
}
