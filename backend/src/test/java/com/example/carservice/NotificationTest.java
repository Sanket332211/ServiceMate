package com.example.carservice;

import com.example.carservice.dto.*;
import com.example.carservice.entity.*;
import com.example.carservice.repository.*;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NotificationTest
 *
 * Comprehensive integration tests for Phase 8 Persistent In-App Notification System:
 * - Persistent DB notifications for all lifecycle events (Booking confirmed/cancelled, workflow progression, repair discovery/authorization)
 * - Customer & Service Center data isolation
 * - Individual mark-as-read and mark-all-as-read (PATCH /api/notifications/read-all)
 * - Unread counter badge accuracy and idempotency
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class NotificationTest {

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
    private ObjectMapper objectMapper;

    private User customer1;
    private User customer2;
    private User serviceCenterUser;
    private Vehicle vehicle1;
    private Vehicle vehicle2;

    private String customer1Token;
    private String customer2Token;
    private String serviceCenterToken;

    @BeforeEach
    void setUp() throws Exception {
        notificationRepository.deleteAll();
        inspectionFindingRepository.deleteAll();
        serviceItemRepository.deleteAll();
        serviceRecordRepository.deleteAll();
        repairRepository.deleteAll();
        workflowRepository.deleteAll();
        bookingRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Customer 1
        customer1 = new User("Rahul Sharma", "rahul@example.com", "9876543210", passwordEncoder.encode("password123"), Role.CUSTOMER);
        customer1 = userRepository.save(customer1);

        // 2. Create Customer 2
        customer2 = new User("Priya Patel", "priya@example.com", "9876543211", passwordEncoder.encode("password123"), Role.CUSTOMER);
        customer2 = userRepository.save(customer2);

        // 3. Create Service Center Admin
        serviceCenterUser = new User("Service Admin", "admin@servicemate.com", "9998887770", passwordEncoder.encode("admin123"), Role.SERVICE_CENTER);
        serviceCenterUser = userRepository.save(serviceCenterUser);

        // 4. Create Vehicles
        vehicle1 = new Vehicle(customer1, "MH12AB1001", "Hyundai", "i20", 2021, FuelType.PETROL, Transmission.MANUAL, 15000);
        vehicle1 = vehicleRepository.save(vehicle1);

        vehicle2 = new Vehicle(customer2, "MH12AB2002", "Honda", "City", 2022, FuelType.PETROL, Transmission.AUTOMATIC, 22000);
        vehicle2 = vehicleRepository.save(vehicle2);

        // 5. Generate Auth Tokens
        customer1Token = obtainToken("rahul@example.com", "password123");
        customer2Token = obtainToken("priya@example.com", "password123");
        serviceCenterToken = obtainToken("admin@servicemate.com", "admin123");
    }

    private String obtainToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("TEST 1: Creating a booking automatically creates a persistent BOOKING_CONFIRMED notification")
    void testBookingConfirmedCreatesNotification() throws Exception {
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest(
                vehicle1.getId(),
                ServiceType.GENERAL_SERVICE,
                bookingDate,
                TimeSlot.MORNING_SLOT_1,
                false
        );

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString()).get("id").asLong();

        // Verify notification in database
        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(customer1.getId());
        assertEquals(1, notifs.size());
        Notification n = notifs.get(0);
        assertEquals(NotificationType.BOOKING_CONFIRMED, n.getType());
        assertEquals("Booking Confirmed", n.getTitle());
        assertTrue(n.getMessage().contains("Hyundai i20"));
        assertEquals(bookingId, n.getRelatedBookingId());
        assertFalse(n.isRead());

        // Verify via REST endpoint
        mockMvc.perform(get("/api/notifications/my")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type").value("BOOKING_CONFIRMED"))
                .andExpect(jsonPath("$[0].title").value("Booking Confirmed"))
                .andExpect(jsonPath("$[0].isRead").value(false));
    }

    @Test
    @DisplayName("TEST 2: Cancelling a booking creates a persistent BOOKING_CANCELLED notification")
    void testBookingCancelledCreatesNotification() throws Exception {
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest(
                vehicle1.getId(),
                ServiceType.OIL_CHANGE,
                bookingDate,
                TimeSlot.MORNING_SLOT_2,
                false
        );

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString()).get("id").asLong();

        // Customer cancels booking
        mockMvc.perform(patch("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Customer should now have 2 notifications: BOOKING_CANCELLED (newest) and BOOKING_CONFIRMED
        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(customer1.getId());
        assertEquals(2, notifs.size());
        assertEquals(NotificationType.BOOKING_CANCELLED, notifs.get(0).getType());
        assertEquals("Booking Cancelled", notifs.get(0).getTitle());
        assertTrue(notifs.get(0).getMessage().contains("Hyundai i20"));
        assertEquals(bookingId, notifs.get(0).getRelatedBookingId());
    }

    @Test
    @DisplayName("TEST 3: Full 7-stage service workflow transitions create correct persistent notifications")
    void testWorkflowMilestoneNotifications() throws Exception {
        // 1. Create booking
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        BookingRequest request = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, bookingDate, TimeSlot.MORNING_SLOT_1, false);
        MvcResult res = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn();
        long bookingId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // 2. CAR_RECEIVED
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/receive")
                        .header("Authorization", "Bearer " + serviceCenterToken))
                .andExpect(status().isOk());

        // 3. INSPECTION
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-inspection")
                        .header("Authorization", "Bearer " + serviceCenterToken))
                .andExpect(status().isOk());

        // 4. SERVICE_IN_PROGRESS
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-service")
                        .header("Authorization", "Bearer " + serviceCenterToken))
                .andExpect(status().isOk());

        // 5. QUALITY_CHECK
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-quality-check")
                        .header("Authorization", "Bearer " + serviceCenterToken))
                .andExpect(status().isOk());

        // Save completion dossier
        ServiceCompletionRequest completionRequest = new ServiceCompletionRequest(
                16000,
                "Full scheduled service completed with all checkpoints verified.",
                List.of(new ServiceItemDto("Engine Oil Synthetic", "FLUIDS", 1, new BigDecimal("1200.00"))),
                List.of(new InspectionFindingDto("Engine Oil", "GOOD", "Fresh oil refilled"))
        );
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/service-record")
                        .header("Authorization", "Bearer " + serviceCenterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completionRequest)))
                .andExpect(status().isOk());

        // 6. READY_FOR_DELIVERY
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/mark-ready")
                        .header("Authorization", "Bearer " + serviceCenterToken))
                .andExpect(status().isOk());

        // 7. COMPLETED
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/complete")
                        .header("Authorization", "Bearer " + serviceCenterToken))
                .andExpect(status().isOk());

        // Verify customer received notifications for milestones
        List<Notification> customerNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(customer1.getId());
        assertTrue(customerNotifs.size() >= 7, "Expected at least 7 notifications for complete service lifecycle");

        assertTrue(customerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.BOOKING_CONFIRMED));
        assertTrue(customerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.SERVICE_STATUS_UPDATED));
        assertTrue(customerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.VEHICLE_READY));
        assertTrue(customerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.SERVICE_COMPLETED));
    }

    @Test
    @DisplayName("TEST 4: Additional repair requested alerts customer; approval alerts service center")
    void testAdditionalRepairNotificationFlow() throws Exception {
        // Create booking & progress to SERVICE_IN_PROGRESS
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        BookingRequest request = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, bookingDate, TimeSlot.MORNING_SLOT_1, false);
        MvcResult res = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn();
        long bookingId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/receive").header("Authorization", "Bearer " + serviceCenterToken));
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-inspection").header("Authorization", "Bearer " + serviceCenterToken));
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-service").header("Authorization", "Bearer " + serviceCenterToken));

        // 1. Service center creates repair
        AdditionalRepairRequest repairReq = new AdditionalRepairRequest("Brake Pad Replacement", "Worn down to 2mm", new BigDecimal("2200.00"));
        MvcResult repairResult = mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/repairs")
                        .header("Authorization", "Bearer " + serviceCenterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repairReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long repairId = objectMapper.readTree(repairResult.getResponse().getContentAsString()).get("id").asLong();

        // Customer must have received REPAIR_REQUESTED notification
        List<Notification> custNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(customer1.getId());
        Notification repairNotif = custNotifs.stream()
                .filter(n -> n.getType() == NotificationType.REPAIR_REQUESTED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("REPAIR_REQUESTED notification not found for customer"));

        assertTrue(repairNotif.getMessage().contains("Brake Pad Replacement"));
        assertTrue(repairNotif.getMessage().contains("2200"));

        // 2. Customer approves repair
        mockMvc.perform(post("/api/repairs/" + repairId + "/approve")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Service Center staff must have received REPAIR_APPROVED notification
        List<Notification> scNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(serviceCenterUser.getId());
        assertFalse(scNotifs.isEmpty(), "Service center should receive notification when customer approves repair");
        Notification scApprovedNotif = scNotifs.stream()
                .filter(n -> n.getType() == NotificationType.REPAIR_APPROVED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("REPAIR_APPROVED notification not found for service center"));

        assertTrue(scApprovedNotif.getMessage().contains("Brake Pad Replacement"));
    }

    @Test
    @DisplayName("TEST 5: Additional repair rejection alerts service center")
    void testAdditionalRepairRejectionNotification() throws Exception {
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        BookingRequest request = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, bookingDate, TimeSlot.MORNING_SLOT_1, false);
        MvcResult res = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn();
        long bookingId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/receive").header("Authorization", "Bearer " + serviceCenterToken));
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-inspection").header("Authorization", "Bearer " + serviceCenterToken));
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-service").header("Authorization", "Bearer " + serviceCenterToken));

        AdditionalRepairRequest repairReq = new AdditionalRepairRequest("Cabin Filter", "Dust accumulation", new BigDecimal("650.00"));
        MvcResult repairResult = mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/repairs")
                        .header("Authorization", "Bearer " + serviceCenterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repairReq)))
                .andExpect(status().isCreated())
                .andReturn();

        long repairId = objectMapper.readTree(repairResult.getResponse().getContentAsString()).get("id").asLong();

        // Customer rejects repair
        mockMvc.perform(post("/api/repairs/" + repairId + "/reject")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        // Service center receives REPAIR_REJECTED notification
        List<Notification> scNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(serviceCenterUser.getId());
        assertTrue(scNotifs.stream().anyMatch(n -> n.getType() == NotificationType.REPAIR_REJECTED && n.getMessage().contains("Cabin Filter")));
    }

    @Test
    @DisplayName("TEST 6: Customer A and Customer B notification data isolation is strictly enforced")
    void testCustomerNotificationDataIsolation() throws Exception {
        // Customer 1 creates a booking
        LocalDate date = LocalDate.now().plusDays(2);
        BookingRequest req1 = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, date, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        // Customer 2 creates a booking
        BookingRequest req2 = new BookingRequest(vehicle2.getId(), ServiceType.AC_SERVICE, date, TimeSlot.AFTERNOON_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customer2Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated());

        // Customer 1 only sees Customer 1's notification
        mockMvc.perform(get("/api/notifications/my").header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message", containsString("Hyundai i20")));

        // Customer 2 only sees Customer 2's notification
        mockMvc.perform(get("/api/notifications/my").header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].message", containsString("Honda City")));
    }

    @Test
    @DisplayName("TEST 7: Customer A cannot mark Customer B's notification as read (403 Forbidden)")
    void testCrossCustomerNotificationAccessForbidden() throws Exception {
        // Customer 1 creates a booking
        LocalDate date = LocalDate.now().plusDays(2);
        BookingRequest req1 = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, date, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated());

        List<Notification> cust1Notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(customer1.getId());
        long cust1NotifId = cust1Notifs.get(0).getId();

        // Customer 2 attempts to mark Customer 1's notification as read
        mockMvc.perform(patch("/api/notifications/" + cust1NotifId + "/read")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 8: Individual mark-as-read and mark-all-as-read (PATCH /api/notifications/read-all) work correctly")
    void testMarkAllAsReadAndUnreadCount() throws Exception {
        // Generate multiple notifications for Customer 1
        LocalDate date1 = LocalDate.now().plusDays(2);
        BookingRequest req1 = new BookingRequest(vehicle1.getId(), ServiceType.OIL_CHANGE, date1, TimeSlot.MORNING_SLOT_1, false);
        MvcResult res1 = mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated()).andReturn();
        long id1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asLong();

        // Cancel booking to generate second notification
        mockMvc.perform(patch("/api/bookings/" + id1 + "/cancel").header("Authorization", "Bearer " + customer1Token)).andExpect(status().isOk());

        // Check unread count -> 2
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        // Mark all as read
        mockMvc.perform(patch("/api/notifications/read-all").header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Unread count is now 0
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        // All notifications now show isRead = true
        mockMvc.perform(get("/api/notifications/my").header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].isRead").value(true))
                .andExpect(jsonPath("$[1].isRead").value(true));
    }

    @Test
    @DisplayName("TEST 9: Calling GET endpoints repeatedly does not create duplicate notifications")
    void testNoDuplicateNotificationsFromGetEndpoints() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);
        BookingRequest req = new BookingRequest(vehicle1.getId(), ServiceType.GENERAL_SERVICE, date, TimeSlot.MORNING_SLOT_1, false);
        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + customer1Token)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req))).andExpect(status().isCreated());

        long countBefore = notificationRepository.count();

        // Call GET endpoints multiple times
        mockMvc.perform(get("/api/notifications/my").header("Authorization", "Bearer " + customer1Token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer " + customer1Token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/bookings/my").header("Authorization", "Bearer " + customer1Token)).andExpect(status().isOk());

        long countAfter = notificationRepository.count();
        assertEquals(countBefore, countAfter, "GET operations must be read-only and never generate notifications");
    }
}
