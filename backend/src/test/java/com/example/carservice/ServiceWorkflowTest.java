package com.example.carservice;

import com.example.carservice.dto.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ServiceWorkflowTest
 *
 * Automated integration tests for Phase 5: Service Center Operations & Real-Time Service Workflow.
 * Verifies the 7-milestone state machine, multi-repair resolution, customer authorization, and security.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ServiceWorkflowTest {

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
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @Autowired
    private InspectionFindingRepository inspectionFindingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User admin;
    private User customer1;
    private User customer2;
    private Vehicle vehicle1;
    private Vehicle vehicle2;
    private String adminToken;
    private String customer1Token;
    private String customer2Token;
    private ServiceBooking booking1;
    private ServiceBooking booking2;

    @BeforeEach
    void setUp() {
        serviceItemRepository.deleteAll();
        inspectionFindingRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        notificationRepository.deleteAll();
        repairRepository.deleteAll();
        workflowRepository.deleteAll();
        bookingRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        // Service Center Admin
        admin = new User("Workshop Admin", "admin@servicemate.com", "9999999999", passwordEncoder.encode("AdminPass123!"), Role.SERVICE_CENTER);
        admin = userRepository.save(admin);
        adminToken = jwtService.generateToken(admin);

        // Customer 1
        customer1 = new User("Rahul Sharma", "rahul@example.com", "9876543210", passwordEncoder.encode("Password123!"), Role.CUSTOMER);
        customer1 = userRepository.save(customer1);
        customer1Token = jwtService.generateToken(customer1);

        vehicle1 = new Vehicle(customer1, "MH12AB1001", "Honda", "City", 2021, FuelType.PETROL, Transmission.AUTOMATIC, 25000);
        vehicle1 = vehicleRepository.save(vehicle1);

        // Customer 2
        customer2 = new User("Priya Patel", "priya@example.com", "9876543211", passwordEncoder.encode("Password123!"), Role.CUSTOMER);
        customer2 = userRepository.save(customer2);
        customer2Token = jwtService.generateToken(customer2);

        vehicle2 = new Vehicle(customer2, "MH12AB2002", "Hyundai", "i20", 2022, FuelType.PETROL, Transmission.MANUAL, 18000);
        vehicle2 = vehicleRepository.save(vehicle2);

        // Active confirmed booking for Customer 1
        booking1 = new ServiceBooking(
                customer1,
                vehicle1,
                ServiceType.GENERAL_SERVICE,
                LocalDate.now().plusDays(1),
                TimeSlot.MORNING_SLOT_1,
                BookingStatus.CONFIRMED,
                false,
                0,
                1499,
                1499
        );
        booking1 = bookingRepository.save(booking1);

        // Active confirmed booking for Customer 2
        booking2 = new ServiceBooking(
                customer2,
                vehicle2,
                ServiceType.OIL_CHANGE,
                LocalDate.now().plusDays(1),
                TimeSlot.MORNING_SLOT_2,
                BookingStatus.CONFIRMED,
                true,
                300,
                999,
                1299
        );
        booking2 = bookingRepository.save(booking2);
    }

    @Test
    @DisplayName("TEST 1: Service Center marks car received (CONFIRMED -> CAR_RECEIVED)")
    void testReceiveVehicle() throws Exception {
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/receive")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("CAR_RECEIVED"))
                .andExpect(jsonPath("$.carReceivedAt").isNotEmpty())
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));

        ServiceWorkflow workflow = workflowRepository.findByBookingId(booking1.getId()).orElse(null);
        assertNotNull(workflow);
        assertEquals(WorkflowStatus.CAR_RECEIVED, workflow.getStatus());
    }

    @Test
    @DisplayName("TEST 2: Sequential service progression: CAR_RECEIVED -> INSPECTION -> SERVICE_IN_PROGRESS")
    void testWorkflowProgressionThroughInspectionAndService() throws Exception {
        // 1. Receive
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/receive")
                .header("Authorization", "Bearer " + adminToken));

        // 2. Start Inspection
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/start-inspection")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("INSPECTION"))
                .andExpect(jsonPath("$.inspectionStartedAt").isNotEmpty());

        // 3. Start Service
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/start-service")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("SERVICE_IN_PROGRESS"))
                .andExpect(jsonPath("$.serviceStartedAt").isNotEmpty());
    }

    @Test
    @DisplayName("TEST 3: Quality check and ready for delivery transitions")
    void testQualityCheckAndReadyForDelivery() throws Exception {
        advanceWorkflowTo(booking1.getId(), WorkflowStatus.SERVICE_IN_PROGRESS);

        // 4. Quality Check
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/start-quality-check")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("QUALITY_CHECK"))
                .andExpect(jsonPath("$.qualityCheckStartedAt").isNotEmpty());

        // Submit completion details
        submitTestCompletionDetails(booking1.getId());

        // 5. Mark Ready
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/mark-ready")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("READY_FOR_DELIVERY"))
                .andExpect(jsonPath("$.readyForDeliveryAt").isNotEmpty());
    }

    @Test
    @DisplayName("TEST 4: Completing workflow updates associated ServiceBooking.status to COMPLETED")
    void testCompleteServiceUpdatesBookingStatus() throws Exception {
        advanceWorkflowTo(booking1.getId(), WorkflowStatus.READY_FOR_DELIVERY);

        // 6. Complete
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/complete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.bookingStatus").value("COMPLETED"));

        ServiceBooking updatedBooking = bookingRepository.findById(booking1.getId()).orElseThrow();
        assertEquals(BookingStatus.COMPLETED, updatedBooking.getStatus());
    }

    @Test
    @DisplayName("TEST 5: Invalid backward transition is rejected with 400 Bad Request")
    void testInvalidBackwardTransitionRejected() throws Exception {
        advanceWorkflowTo(booking1.getId(), WorkflowStatus.QUALITY_CHECK);

        // Attempting to move backward to INSPECTION
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/start-inspection")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("must be in CAR_RECEIVED state")));
    }

    @Test
    @DisplayName("TEST 6: Cancelled booking cannot enter the service workflow (400 Bad Request)")
    void testCancelledBookingCannotEnterWorkflow() throws Exception {
        booking1.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking1);

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/receive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cancelled bookings cannot enter")));
    }

    @Test
    @DisplayName("TEST 7: Additional repair creation only allowed in SERVICE_IN_PROGRESS / AWAITING_APPROVAL")
    void testAdditionalRepairCreationRules() throws Exception {
        // Attempting creation before service in progress (in CAR_RECEIVED) -> Rejected
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/receive")
                .header("Authorization", "Bearer " + adminToken));

        AdditionalRepairRequest repairReq = new AdditionalRepairRequest(
                "Front Brake Pad Replacement",
                "Severe brake pad wear below 2mm safety threshold.",
                new BigDecimal("2200.00")
        );

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/repairs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repairReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("only be created during SERVICE_IN_PROGRESS")));

        // Advance to SERVICE_IN_PROGRESS and create repair -> Succeeded, workflow moves to AWAITING_APPROVAL
        advanceWorkflowTo(booking1.getId(), WorkflowStatus.SERVICE_IN_PROGRESS);

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/repairs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repairReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.estimatedAmount").value(2200.00));

        ServiceWorkflow workflow = workflowRepository.findByBookingId(booking1.getId()).orElseThrow();
        assertEquals(WorkflowStatus.AWAITING_APPROVAL, workflow.getStatus());
    }

    @Test
    @DisplayName("TEST 8: Customer can approve own pending repair (resumes SERVICE_IN_PROGRESS)")
    void testCustomerApproveRepair() throws Exception {
        Long repairId = createTestRepair(booking1.getId(), "Cabin Air Filter", "Clogged filter", new BigDecimal("750.00"));

        mockMvc.perform(post("/api/repairs/" + repairId + "/approve")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.respondedAt").isNotEmpty());

        ServiceWorkflow workflow = workflowRepository.findByBookingId(booking1.getId()).orElseThrow();
        assertEquals(WorkflowStatus.SERVICE_IN_PROGRESS, workflow.getStatus());
    }

    @Test
    @DisplayName("TEST 9: Customer can reject own pending repair (moves to QUALITY_CHECK when all rejected)")
    void testCustomerRejectRepair() throws Exception {
        Long repairId = createTestRepair(booking1.getId(), "Wiper Blade Replacement", "Torn rubber blades", new BigDecimal("450.00"));

        mockMvc.perform(post("/api/repairs/" + repairId + "/reject")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.respondedAt").isNotEmpty());

        ServiceWorkflow workflow = workflowRepository.findByBookingId(booking1.getId()).orElseThrow();
        assertEquals(WorkflowStatus.QUALITY_CHECK, workflow.getStatus());
    }

    @Test
    @DisplayName("TEST 10: Customer A cannot approve/reject Customer B's repair (403 Forbidden)")
    void testCustomerCannotApproveOtherCustomerRepair() throws Exception {
        Long repairId = createTestRepair(booking1.getId(), "Rear Rotor Polish", "Rust build-up", new BigDecimal("1200.00"));

        // Customer 2 tries to approve Customer 1's repair
        mockMvc.perform(post("/api/repairs/" + repairId + "/approve")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 11: Service Center cannot approve repair on behalf of customer (403 Forbidden)")
    void testServiceCenterCannotApproveRepair() throws Exception {
        Long repairId = createTestRepair(booking1.getId(), "Wheel Alignment", "Steering pull", new BigDecimal("800.00"));

        mockMvc.perform(post("/api/repairs/" + repairId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 12: Already resolved repair cannot be modified again (400 Bad Request)")
    void testResolvedRepairCannotBeChangedAgain() throws Exception {
        Long repairId = createTestRepair(booking1.getId(), "Spark Plug Replacement", "Misfire code", new BigDecimal("1500.00"));

        // First approval succeeds
        mockMvc.perform(post("/api/repairs/" + repairId + "/approve")
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        // Attempting to reject already APPROVED repair -> 400 Bad Request
        mockMvc.perform(post("/api/repairs/" + repairId + "/reject")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already been APPROVED")));
    }

    @Test
    @DisplayName("TEST 13: Multiple repairs: Approving first repair keeps workflow in AWAITING_APPROVAL while second is pending")
    void testMultipleRepairs_ApproveFirst_WorkflowRemainsAwaitingApproval() throws Exception {
        advanceWorkflowTo(booking1.getId(), WorkflowStatus.SERVICE_IN_PROGRESS);

        // Create 2 repair requests
        Long repair1Id = createRepairDirectly(booking1.getId(), "Brake Pads", "Worn pads", new BigDecimal("2000.00"));
        Long repair2Id = createRepairDirectly(booking1.getId(), "Battery Replacement", "Low voltage", new BigDecimal("4500.00"));

        // Approve Repair 1
        mockMvc.perform(post("/api/repairs/" + repair1Id + "/approve")
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        // Workflow MUST remain AWAITING_APPROVAL because Repair 2 is still PENDING
        ServiceWorkflow workflow = workflowRepository.findByBookingId(booking1.getId()).orElseThrow();
        assertEquals(WorkflowStatus.AWAITING_APPROVAL, workflow.getStatus());

        // Now resolve Repair 2 (Approved) -> Workflow moves to SERVICE_IN_PROGRESS
        mockMvc.perform(post("/api/repairs/" + repair2Id + "/approve")
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        workflow = workflowRepository.findByBookingId(booking1.getId()).orElseThrow();
        assertEquals(WorkflowStatus.SERVICE_IN_PROGRESS, workflow.getStatus());
    }

    @Test
    @DisplayName("TEST 14: Multiple repairs: Rejecting first repair keeps workflow in AWAITING_APPROVAL while second is pending")
    void testMultipleRepairs_RejectFirst_WorkflowRemainsAwaitingApproval() throws Exception {
        advanceWorkflowTo(booking1.getId(), WorkflowStatus.SERVICE_IN_PROGRESS);

        Long repair1Id = createRepairDirectly(booking1.getId(), "Alloy Wheel Repair", "Minor rim scratch", new BigDecimal("1800.00"));
        Long repair2Id = createRepairDirectly(booking1.getId(), "AC Gas Refill", "Low cooling pressure", new BigDecimal("1200.00"));

        // Reject Repair 1
        mockMvc.perform(post("/api/repairs/" + repair1Id + "/reject")
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        // Workflow MUST remain AWAITING_APPROVAL because Repair 2 is still PENDING
        ServiceWorkflow workflow = workflowRepository.findByBookingId(booking1.getId()).orElseThrow();
        assertEquals(WorkflowStatus.AWAITING_APPROVAL, workflow.getStatus());

        // Reject Repair 2 -> Since ALL repairs were REJECTED, workflow advances to QUALITY_CHECK
        mockMvc.perform(post("/api/repairs/" + repair2Id + "/reject")
                .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        workflow = workflowRepository.findByBookingId(booking1.getId()).orElseThrow();
        assertEquals(WorkflowStatus.QUALITY_CHECK, workflow.getStatus());
    }

    @Test
    @DisplayName("TEST 15: Cannot move to QUALITY_CHECK while a repair is pending (400 Bad Request)")
    void testCannotBypassPendingRepair() throws Exception {
        createTestRepair(booking1.getId(), "Transmission Fluid Flush", "Burnt fluid color", new BigDecimal("3200.00"));

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/start-quality-check")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("awaiting customer approval")));
    }

    @Test
    @DisplayName("TEST 16: Customer cannot access other customer's workflow details (403 Forbidden)")
    void testCustomerCannotAccessOtherCustomerWorkflow() throws Exception {
        advanceWorkflowTo(booking1.getId(), WorkflowStatus.INSPECTION);

        // Customer 2 attempts to query Customer 1's workflow
        mockMvc.perform(get("/api/bookings/" + booking1.getId() + "/workflow")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 17: In-app notification creation and mark-as-read flow")
    void testNotificationCreationAndMarkRead() throws Exception {
        // Creating a repair generates a notification for Customer 1
        createTestRepair(booking1.getId(), "Headlight Bulb", "Blown filament", new BigDecimal("350.00"));

        MvcResult result = mockMvc.perform(get("/api/notifications/my")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].type").value("REPAIR_REQUESTED"))
                .andExpect(jsonPath("$[0].isRead").value(false))
                .andReturn();

        Long notificationId = objectMapper.readTree(result.getResponse().getContentAsString()).get(0).get("id").asLong();

        // Mark as read
        mockMvc.perform(patch("/api/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("TEST 18: Customer cancels booking immediately -> status is CANCELLED and all workflow transitions are blocked")
    void testImmediateCancellationPreventsAllWorkflowAdvancement() throws Exception {
        // 1. Customer cancels booking1
        mockMvc.perform(patch("/api/bookings/" + booking1.getId() + "/cancel")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 2. GET booking details returns status CANCELLED
        mockMvc.perform(get("/api/bookings/" + booking1.getId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 3. GET workflow returns bookingStatus CANCELLED
        mockMvc.perform(get("/api/bookings/" + booking1.getId() + "/workflow")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CANCELLED"));

        // 4. Workshop Admin cannot receive a cancelled vehicle
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/receive")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 5. Workshop Admin cannot advance cancelled booking to inspection
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/start-inspection")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 6. Workshop Admin cannot advance cancelled booking to service in progress
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/start-service")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 7. Workshop Admin cannot advance cancelled booking to quality check
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/start-quality-check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 8. Workshop Admin cannot mark cancelled booking ready for delivery
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/mark-ready")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 9. Workshop Admin cannot complete cancelled booking
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/complete")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 19: Service Center bookings list includes cancelled bookings identifiable with CANCELLED status")
    void testServiceCenterQueueIncludesCancelledBookingsWithCorrectStatus() throws Exception {
        // Cancel booking1
        mockMvc.perform(patch("/api/bookings/" + booking1.getId() + "/cancel")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk());

        // Service Center fetches all workflows
        mockMvc.perform(get("/api/service-center/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.bookingId == " + booking1.getId() + ")].bookingStatus").value(hasItem("CANCELLED")))
                .andExpect(jsonPath("$[?(@.bookingId == " + booking2.getId() + ")].bookingStatus").value(hasItem("CONFIRMED")));
    }

    @Test
    @DisplayName("TEST 20: Service Center Workshop Queue returns most recently created bookings first")
    void testServiceCenterQueueReturnsNewestBookingsFirst() throws Exception {
        // Create booking 3 for vehicle 1
        ServiceBooking booking3 = new ServiceBooking(
                customer1,
                vehicle1,
                ServiceType.BATTERY_SERVICE,
                LocalDate.now().plusDays(4),
                TimeSlot.AFTERNOON_SLOT_2,
                BookingStatus.CONFIRMED,
                false,
                0,
                5500,
                5500
        );
        booking3 = bookingRepository.save(booking3);

        // GET /api/service-center/bookings -> [booking3, booking2, booking1]
        mockMvc.perform(get("/api/service-center/bookings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].bookingId").value(booking3.getId()))
                .andExpect(jsonPath("$[1].bookingId").value(booking2.getId()))
                .andExpect(jsonPath("$[2].bookingId").value(booking1.getId()));
    }

    // Helper methods
    private void advanceWorkflowTo(Long bookingId, WorkflowStatus targetStatus) throws Exception {
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/receive").header("Authorization", "Bearer " + adminToken));
        if (targetStatus == WorkflowStatus.CAR_RECEIVED) return;

        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-inspection").header("Authorization", "Bearer " + adminToken));
        if (targetStatus == WorkflowStatus.INSPECTION) return;

        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-service").header("Authorization", "Bearer " + adminToken));
        if (targetStatus == WorkflowStatus.SERVICE_IN_PROGRESS) return;

        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-quality-check").header("Authorization", "Bearer " + adminToken));
        if (targetStatus == WorkflowStatus.QUALITY_CHECK) return;

        submitTestCompletionDetails(bookingId);

        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/mark-ready").header("Authorization", "Bearer " + adminToken));
        if (targetStatus == WorkflowStatus.READY_FOR_DELIVERY) return;

        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/complete").header("Authorization", "Bearer " + adminToken));
    }

    private void submitTestCompletionDetails(Long bookingId) throws Exception {
        int currentMil = 26000;

        List<ServiceItemDto> items = List.of(
                new ServiceItemDto("Engine Oil", "FLUIDS", 1, new BigDecimal("800.00")),
                new ServiceItemDto("Oil Filter", "PARTS", 1, new BigDecimal("350.00")),
                new ServiceItemDto("Labour", "LABOUR", 1, new BigDecimal("349.00"))
        );
        List<InspectionFindingDto> findings = List.of(
                new InspectionFindingDto("Brakes", "Good", "Front pads 6mm"),
                new InspectionFindingDto("Battery", "Good", "12.6V")
        );
        ServiceCompletionRequest req = new ServiceCompletionRequest(currentMil, "Service and inspection complete.", items, findings);

        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/service-record")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    private Long createTestRepair(Long bookingId, String desc, String reason, BigDecimal amount) throws Exception {
        advanceWorkflowTo(bookingId, WorkflowStatus.SERVICE_IN_PROGRESS);
        return createRepairDirectly(bookingId, desc, reason, amount);
    }

    private Long createRepairDirectly(Long bookingId, String desc, String reason, BigDecimal amount) throws Exception {
        AdditionalRepairRequest repairReq = new AdditionalRepairRequest(desc, reason, amount);
        MvcResult res = mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/repairs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repairReq)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }
}
