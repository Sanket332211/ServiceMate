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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ServiceHistoryTest
 *
 * Automated integration tests for Phase 6: Complete Portable Vehicle Service History & Professional PDF Generation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ServiceHistoryTest {

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

        // Active confirmed booking for Customer 1 with pickup/drop
        booking1 = new ServiceBooking(
                customer1,
                vehicle1,
                ServiceType.GENERAL_SERVICE,
                LocalDate.now().plusDays(1),
                TimeSlot.MORNING_SLOT_1,
                BookingStatus.CONFIRMED,
                true,
                300,
                1499,
                1799
        );
        booking1 = bookingRepository.save(booking1);

        // Active confirmed booking for Customer 2 without pickup/drop
        booking2 = new ServiceBooking(
                customer2,
                vehicle2,
                ServiceType.OIL_CHANGE,
                LocalDate.now().plusDays(1),
                TimeSlot.MORNING_SLOT_2,
                BookingStatus.CONFIRMED,
                false,
                0,
                999,
                999
        );
        booking2 = bookingRepository.save(booking2);
    }

    @Test
    @DisplayName("TEST 1: Service Center enters service completion details during QUALITY_CHECK with backend calculation")
    void testSaveCompletionDetailsDuringQualityCheck() throws Exception {
        advanceWorkflowToQualityCheck(booking1.getId());

        ServiceCompletionRequest request = createSampleCompletionRequest(26500);

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/service-record")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mileage").value(26500))
                .andExpect(jsonPath("$.serviceSummary").value(containsString("Full synthetic engine oil replaced")))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.actualBaseServiceAmount").value(1499.00))
                .andExpect(jsonPath("$.pickupDropUsed").value(true))
                .andExpect(jsonPath("$.pickupDropCharge").value(300.00))
                .andExpect(jsonPath("$.actualTotalAmount").value(1799.00)); // 1499 + 0 + 300
    }

    @Test
    @DisplayName("TEST 2: Cannot advance from QUALITY_CHECK to READY_FOR_DELIVERY without completion details (400 Bad Request)")
    void testCannotMarkReadyWithoutCompletionDetails() throws Exception {
        advanceWorkflowToQualityCheck(booking1.getId());

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/mark-ready")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Service completion details are incomplete")));
    }

    @Test
    @DisplayName("TEST 3: Marking READY_FOR_DELIVERY succeeds when valid completion details exist")
    void testMarkReadySucceedsWithCompletionDetails() throws Exception {
        advanceWorkflowToQualityCheck(booking1.getId());

        ServiceCompletionRequest request = createSampleCompletionRequest(26000);
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/service-record")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/mark-ready")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("READY_FOR_DELIVERY"));
    }

    @Test
    @DisplayName("TEST 4: Completing service finalizes ServiceRecord and updates Vehicle odometer mileage")
    void testCompleteServiceFinalizesRecordAndUpdatesOdometer() throws Exception {
        advanceWorkflowToQualityCheck(booking1.getId());

        ServiceCompletionRequest request = createSampleCompletionRequest(27500);
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/service-record")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/mark-ready")
                .header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/complete")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("COMPLETED"));

        // Verify ServiceRecord finalization
        ServiceRecord record = serviceRecordRepository.findByBookingId(booking1.getId()).orElseThrow();
        assertNotNull(record.getFinalizedAt());
        assertEquals(27500, record.getMileage());

        // Verify Vehicle currentMileage update
        Vehicle updatedVehicle = vehicleRepository.findById(vehicle1.getId()).orElseThrow();
        assertEquals(27500, updatedVehicle.getCurrentMileage());
    }

    @Test
    @DisplayName("TEST 5: Entered mileage lower than vehicle's stored mileage is rejected (400 Bad Request)")
    void testLowerMileageRejected() throws Exception {
        advanceWorkflowToQualityCheck(booking1.getId());

        // Vehicle has 25,000 km -> entering 24,000 km
        ServiceCompletionRequest request = createSampleCompletionRequest(24000);

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/service-record")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot be lower than the vehicle's current stored mileage")));
    }

    @Test
    @DisplayName("TEST 6: Rejected additional repairs are excluded from actual total cost, approved repairs included")
    void testActualCostCalculationWithApprovedAndRejectedRepairs() throws Exception {
        // Start workflow to SERVICE_IN_PROGRESS
        advanceWorkflowToServiceInProgress(booking1.getId());

        // Create 2 additional repairs
        Long repair1Id = createRepair(booking1.getId(), "Front Brake Pad Replacement", "Worn below 2mm", new BigDecimal("2000.00"));
        Long repair2Id = createRepair(booking1.getId(), "Battery Replacement", "Low voltage", new BigDecimal("4500.00"));

        // Customer approves Repair 1 and rejects Repair 2
        mockMvc.perform(post("/api/repairs/" + repair1Id + "/approve").header("Authorization", "Bearer " + customer1Token));
        mockMvc.perform(post("/api/repairs/" + repair2Id + "/reject").header("Authorization", "Bearer " + customer1Token));

        // Workflow resumes to SERVICE_IN_PROGRESS -> move to QUALITY_CHECK
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/start-quality-check")
                .header("Authorization", "Bearer " + adminToken));

        // Submit completion details with base items = ₹1499
        ServiceCompletionRequest request = createSampleCompletionRequest(26000);
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/service-record")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualBaseServiceAmount").value(1499.00))
                .andExpect(jsonPath("$.actualAdditionalRepairsAmount").value(2000.00)) // Only approved ₹2000
                .andExpect(jsonPath("$.pickupDropCharge").value(300.00))
                .andExpect(jsonPath("$.actualTotalAmount").value(3799.00)) // 1499 + 2000 + 300 = 3799 (4500 rejected excluded)
                .andExpect(jsonPath("$.additionalRepairs", hasSize(2)));
    }

    @Test
    @DisplayName("TEST 7: Finalized service record is immutable and cannot be modified (400 Bad Request)")
    void testFinalizedRecordIsImmutable() throws Exception {
        completeFullServiceFlow(booking1.getId(), 28000);

        ServiceCompletionRequest editRequest = createSampleCompletionRequest(29000);
        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/service-record")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("immutable and cannot be modified")));
    }

    @Test
    @DisplayName("TEST 8: Customer can view own vehicle service history and single record")
    void testCustomerCanViewOwnHistory() throws Exception {
        completeFullServiceFlow(booking1.getId(), 27000);

        // View complete history for vehicle 1
        mockMvc.perform(get("/api/vehicles/" + vehicle1.getId() + "/service-history")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleRegistrationNumber").value("MH12AB1001"))
                .andExpect(jsonPath("$.totalCompletedVisits").value(1))
                .andExpect(jsonPath("$.records", hasSize(1)));

        ServiceRecord record = serviceRecordRepository.findByBookingId(booking1.getId()).orElseThrow();

        // View single record
        mockMvc.perform(get("/api/service-records/" + record.getId())
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(record.getId()))
                .andExpect(jsonPath("$.mileage").value(27000));
    }

    @Test
    @DisplayName("TEST 9: Customer cannot view another customer's vehicle history or service record (403 Forbidden)")
    void testCustomerCannotViewOtherCustomerHistory() throws Exception {
        completeFullServiceFlow(booking1.getId(), 27000);

        // Customer 2 attempts to query Customer 1's vehicle history
        mockMvc.perform(get("/api/vehicles/" + vehicle1.getId() + "/service-history")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());

        ServiceRecord record = serviceRecordRepository.findByBookingId(booking1.getId()).orElseThrow();

        // Customer 2 attempts to query Customer 1's service record
        mockMvc.perform(get("/api/service-records/" + record.getId())
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 10: Single service PDF generation returns valid application/pdf byte stream")
    void testSingleServicePdfGeneration() throws Exception {
        completeFullServiceFlow(booking1.getId(), 28000);
        ServiceRecord record = serviceRecordRepository.findByBookingId(booking1.getId()).orElseThrow();

        MvcResult result = mockMvc.perform(get("/api/service-records/" + record.getId() + "/pdf")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("ServiceMate_Service_Record_")))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 500);
        // PDF header magic bytes %PDF-
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    @DisplayName("TEST 11: Complete vehicle history PDF generation returns valid application/pdf byte stream")
    void testCompleteVehicleHistoryPdfGeneration() throws Exception {
        completeFullServiceFlow(booking1.getId(), 28500);

        MvcResult result = mockMvc.perform(get("/api/vehicles/" + vehicle1.getId() + "/service-history/pdf")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("ServiceMate_Vehicle_History_")))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 500);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    @DisplayName("TEST 12: Customer cannot download another customer's PDF (403 Forbidden)")
    void testCustomerCannotDownloadOtherCustomerPdf() throws Exception {
        completeFullServiceFlow(booking1.getId(), 28000);
        ServiceRecord record = serviceRecordRepository.findByBookingId(booking1.getId()).orElseThrow();

        mockMvc.perform(get("/api/service-records/" + record.getId() + "/pdf")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/vehicles/" + vehicle1.getId() + "/service-history/pdf")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 13: Attempting to generate history PDF for vehicle with no completed history returns 404 Not Found")
    void testEmptyVehicleHistoryPdfReturns404() throws Exception {
        // Vehicle 2 has no completed services
        mockMvc.perform(get("/api/vehicles/" + vehicle2.getId() + "/service-history/pdf")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("No completed service history found")));
    }

    @Test
    @DisplayName("TEST 14: Service Center can view any vehicle's service history")
    void testServiceCenterCanViewAnyHistory() throws Exception {
        completeFullServiceFlow(booking1.getId(), 27000);

        mockMvc.perform(get("/api/vehicles/" + vehicle1.getId() + "/service-history")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleRegistrationNumber").value("MH12AB1001"));
    }

    @Test
    @DisplayName("TEST 15: Customer cannot enter service completion details (403 Forbidden)")
    void testCustomerCannotEnterCompletionDetails() throws Exception {
        advanceWorkflowToQualityCheck(booking1.getId());
        ServiceCompletionRequest request = createSampleCompletionRequest(26000);

        mockMvc.perform(post("/api/service-center/bookings/" + booking1.getId() + "/service-record")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // Helper methods
    private void advanceWorkflowToServiceInProgress(Long bookingId) throws Exception {
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/receive").header("Authorization", "Bearer " + adminToken));
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-inspection").header("Authorization", "Bearer " + adminToken));
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-service").header("Authorization", "Bearer " + adminToken));
    }

    private void advanceWorkflowToQualityCheck(Long bookingId) throws Exception {
        advanceWorkflowToServiceInProgress(bookingId);
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/start-quality-check").header("Authorization", "Bearer " + adminToken));
    }

    private void completeFullServiceFlow(Long bookingId, int mileage) throws Exception {
        advanceWorkflowToQualityCheck(bookingId);
        ServiceCompletionRequest request = createSampleCompletionRequest(mileage);
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/service-record")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/mark-ready").header("Authorization", "Bearer " + adminToken));
        mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/complete").header("Authorization", "Bearer " + adminToken));
    }

    private Long createRepair(Long bookingId, String desc, String reason, BigDecimal amount) throws Exception {
        AdditionalRepairRequest repairReq = new AdditionalRepairRequest(desc, reason, amount);
        MvcResult res = mockMvc.perform(post("/api/service-center/bookings/" + bookingId + "/repairs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(repairReq)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    }

    private ServiceCompletionRequest createSampleCompletionRequest(int mileage) {
        List<ServiceItemDto> items = List.of(
                new ServiceItemDto("Full Synthetic Engine Oil (5W-30)", "FLUIDS", 1, new BigDecimal("800.00")),
                new ServiceItemDto("OEM Engine Oil Filter", "PARTS", 1, new BigDecimal("350.00")),
                new ServiceItemDto("Scheduled General Service Labour", "LABOUR", 1, new BigDecimal("349.00"))
        );

        List<InspectionFindingDto> findings = List.of(
                new InspectionFindingDto("Brakes", "Good", "Front pads 6mm, rear shoes 5mm. Cleaned and adjusted."),
                new InspectionFindingDto("Battery", "Good", "12.6V resting voltage, charging system healthy at 14.2V."),
                new InspectionFindingDto("Tyres", "Good", "Tread depth 5.5mm across all 4 tyres, pressure calibrated to 33 PSI."),
                new InspectionFindingDto("Air Conditioning", "Good", "Cabin filter cleaned, vent cooling at 7°C.")
        );

        return new ServiceCompletionRequest(
                mileage,
                "Full synthetic engine oil replaced, OEM filter installed, 40-point diagnostic check passed.",
                items,
                findings
        );
    }
}
