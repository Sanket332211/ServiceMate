package com.example.carservice;

import com.example.carservice.dto.AiServiceAdvisorRequest;
import com.example.carservice.dto.InspectionFindingDto;
import com.example.carservice.dto.LoginRequest;
import com.example.carservice.dto.ServiceCompletionRequest;
import com.example.carservice.dto.ServiceItemDto;
import com.example.carservice.entity.*;
import com.example.carservice.exception.AiServiceUnavailableException;
import com.example.carservice.repository.*;
import com.example.carservice.service.GeminiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AiServiceTest
 *
 * Comprehensive unit and integration test suite for Phase 9 AI features:
 * - AI Service Advisor (POST /api/ai/service-advisor)
 * - AI Service Summary (GET /api/service-records/{id}/ai-summary)
 * - Authorization & Customer data isolation
 * - Input validation
 * - Offline Gemini API mocking and error handling (503 Service Unavailable)
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class AiServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeminiService geminiService;

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
    private ServiceRecordRepository serviceRecordRepository;

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @Autowired
    private InspectionFindingRepository inspectionFindingRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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
    @DisplayName("TEST 1: AI Advisor maps exact enum BRAKE_SERVICE -> BRAKE_SERVICE (₹1,799)")
    void testAiAdvisorExactEnumBrake() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Braking System",
                  "recommendedService": "Brake Inspection & Pad Replacement",
                  "recommendedPackage": "BRAKE_SERVICE",
                  "urgency": "MEDIUM",
                  "explanation": "Squeaking noise during braking indicates pad wear."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "My car makes a high pitched squeaking noise when I apply the brakes.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("BRAKE_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("Brake Service & Fluid"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(1799));
    }

    @Test
    @DisplayName("TEST 2: AI Advisor maps display name 'Brake Service & Fluid' -> BRAKE_SERVICE (₹1,799)")
    void testAiAdvisorDisplayNameBrake() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Braking System",
                  "recommendedService": "Brake Pad & Fluid Renewal",
                  "recommendedPackage": "Brake Service & Fluid",
                  "urgency": "MEDIUM",
                  "explanation": "Brake pad replacement is advised."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "My car makes a high pitched squeaking noise when I apply the brakes.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("BRAKE_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("Brake Service & Fluid"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(1799));
    }

    @Test
    @DisplayName("TEST 3: AI Advisor maps spaced enum 'OIL CHANGE' -> OIL_CHANGE (₹999)")
    void testAiAdvisorSpacedEnumOil() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Engine Lubrication System",
                  "recommendedService": "Synthetic Oil Change",
                  "recommendedPackage": "OIL CHANGE",
                  "urgency": "LOW",
                  "explanation": "Periodic engine oil change is required."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "I want to change my engine oil.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("OIL_CHANGE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("Oil Change"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(999));
    }

    @Test
    @DisplayName("TEST 4: AI Advisor maps display name 'Oil Change' -> OIL_CHANGE (₹999)")
    void testAiAdvisorDisplayNameOil() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Engine Lubrication System",
                  "recommendedService": "Engine Oil Renewal",
                  "recommendedPackage": "Oil Change",
                  "urgency": "LOW",
                  "explanation": "Fresh oil prevents engine friction."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "I want to change my engine oil.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("OIL_CHANGE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("Oil Change"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(999));
    }

    @Test
    @DisplayName("TEST 5: AI Advisor maps display name 'AC Service & Inspection' -> AC_SERVICE (₹1,299)")
    void testAiAdvisorDisplayNameAc() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "HVAC / Climate Control",
                  "recommendedService": "AC Diagnostic & Gas Top-up",
                  "recommendedPackage": "AC Service & Inspection",
                  "urgency": "LOW",
                  "explanation": "Weak cabin cooling suggests refrigerant refill."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "My AC is not cooling.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("AC_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("AC Service & Inspection"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(1299));
    }

    @Test
    @DisplayName("TEST 6: AI Advisor maps display name 'Battery Inspection & Care' -> BATTERY_SERVICE (₹499)")
    void testAiAdvisorDisplayNameBattery() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Electrical System",
                  "recommendedService": "Battery Load Test",
                  "recommendedPackage": "Battery Inspection & Care",
                  "urgency": "HIGH",
                  "explanation": "Hesitant engine cranking points to battery wear."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "My battery is weak and the car is difficult to start.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("BATTERY_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("Battery Inspection & Care"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(499));
    }

    @Test
    @DisplayName("TEST 7: AI Advisor maps display name 'General Service' -> GENERAL_SERVICE (₹1,499)")
    void testAiAdvisorDisplayNameGeneral() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "General Vehicle Diagnostic",
                  "recommendedService": "40-point full vehicle inspection",
                  "recommendedPackage": "General Service",
                  "urgency": "MEDIUM",
                  "explanation": "Periodic general inspection is recommended."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "I want a complete checkup of my car.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("GENERAL_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("General Service"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(1499));
    }

    @Test
    @DisplayName("TEST 8: AI Advisor safely maps TRANSMISSION_SERVICE to GENERAL_SERVICE (₹1,499)")
    void testAiAdvisorUnsupportedTransmissionFallback() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Transmission & Drivetrain",
                  "recommendedService": "Transmission Inspection & Diagnostic Check",
                  "recommendedPackage": "TRANSMISSION_SERVICE",
                  "urgency": "HIGH",
                  "explanation": "Accelerating without forward movement indicates transmission slip. General Service provides comprehensive drivetrain diagnostics."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "My car is accelerating but not moving forward.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("GENERAL_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("General Service"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(1499));
    }

    @Test
    @DisplayName("TEST 9: AI Advisor safely maps unknown package 'XYZ_SERVICE' to GENERAL_SERVICE (₹1,499)")
    void testAiAdvisorUnknownPackageFallback() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Custom System",
                  "recommendedService": "Custom Diagnostic",
                  "recommendedPackage": "XYZ_SERVICE",
                  "urgency": "LOW",
                  "explanation": "Unrecognized package safely falls back to General Service."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "Unusual vibration at high speed.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("GENERAL_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("General Service"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(1499));
    }

    @Test
    @DisplayName("TEST 10: AI Advisor safely defaults to GENERAL_SERVICE when recommendedPackage is omitted")
    void testAiAdvisorOmittedPackageFallback() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Engine System",
                  "recommendedService": "Diagnostic Scan",
                  "urgency": "MEDIUM",
                  "explanation": "A general inspection is suggested."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "Check engine light came on.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("GENERAL_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("General Service"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(1499));
    }

    @Test
    @DisplayName("TEST 11: Backend enforces price protection and ignores Gemini-invented pricing")
    void testAiAdvisorPriceProtection() throws Exception {
        // Even if Gemini attempts to inject a price of 999 for BRAKE_SERVICE
        String mockGeminiJson = """
                {
                  "possibleSystem": "Braking System",
                  "recommendedService": "Brake Service",
                  "recommendedPackage": "BRAKE_SERVICE",
                  "recommendedPackagePrice": 999,
                  "urgency": "HIGH",
                  "explanation": "Brake pads inspection is recommended."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "Brakes feel weak.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("BRAKE_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(1799)); // Authoritative ServiceType price
    }

    @Test
    @DisplayName("TEST 12: AI advice generation does not create any bookings in the database")
    void testAiAdvisorDoesNotCreateBooking() throws Exception {
        long initialBookingCount = bookingRepository.count();

        String mockGeminiJson = """
                {
                  "possibleSystem": "Braking System",
                  "recommendedService": "Brake Service",
                  "recommendedPackage": "BRAKE_SERVICE",
                  "urgency": "MEDIUM",
                  "explanation": "Brake pads inspection is recommended."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "Brakes feel weak.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Verify booking table remained untouched
        org.junit.jupiter.api.Assertions.assertEquals(initialBookingCount, bookingRepository.count());
    }

    @Test
    @DisplayName("TEST 13: AI Advisor verifies specialized AC package when Gemini returns General Service for AC query")
    void testAiAdvisorVerifiesSpecializedAcPackage() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Climate Control System",
                  "recommendedService": "AC Inspection and Service",
                  "recommendedPackage": "General Service",
                  "urgency": "LOW",
                  "explanation": "Routine maintenance of your vehicle's air conditioning system helps ensure efficient cooling."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "i need ac service and inspection");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("AC_SERVICE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("AC Service & Inspection"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(1299));
    }

    @Test
    @DisplayName("TEST 14: AI Advisor verifies specialized Oil Change package when Gemini returns General Service for Oil query")
    void testAiAdvisorVerifiesSpecializedOilPackage() throws Exception {
        String mockGeminiJson = """
                {
                  "possibleSystem": "Engine Lubrication System",
                  "recommendedService": "Engine Oil and Filter Change",
                  "recommendedPackage": "General Service",
                  "urgency": "MEDIUM",
                  "explanation": "Over time, engine oil breaks down and loses its ability to properly lubricate."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJson);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "i have not changed my engine oil for a long time");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPackage").value("OIL_CHANGE"))
                .andExpect(jsonPath("$.recommendedPackageName").value("Oil Change"))
                .andExpect(jsonPath("$.recommendedPackagePrice").value(999));
    }

    @Test
    @DisplayName("TEST 2: AI Service Advisor input validation catches missing fields and short descriptions")
    void testAiServiceAdvisorValidation() throws Exception {
        // Missing vehicleId
        AiServiceAdvisorRequest badReq1 = new AiServiceAdvisorRequest(null, "Valid problem description here");
        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq1)))
                .andExpect(status().isBadRequest());

        // Too short problem description (< 5 chars)
        AiServiceAdvisorRequest badReq2 = new AiServiceAdvisorRequest(vehicle1.getId(), "bad");
        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TEST 3: Customer A cannot request AI advice for Customer B's vehicle (403 Forbidden)")
    void testAiServiceAdvisorUnauthorizedVehicle() throws Exception {
        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "Brake pedal feels spongy");

        // Customer 2 attempts to query Customer 1's vehicle
        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 4: AI Service Advisor returns friendly 503 error when Gemini API is unavailable")
    void testAiServiceAdvisorGeminiFailureHandling() throws Exception {
        when(geminiService.generateJson(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("AI service is temporarily unavailable. Please try again later."));

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "Engine check light is blinking");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("AI service is temporarily unavailable")));
    }

    @Test
    @DisplayName("TEST 5: AI Service Summary successfully generates summary for finalized historical service record")
    void testAiServiceSummarySuccess() throws Exception {
        // Create booking & finalized record
        ServiceBooking booking = new ServiceBooking(customer1, vehicle1, ServiceType.GENERAL_SERVICE, LocalDate.now().minusDays(5), TimeSlot.MORNING_SLOT_1, BookingStatus.CONFIRMED, false, 0, 1800, 1800);
        booking = bookingRepository.save(booking);

        ServiceRecord record = new ServiceRecord();
        record.setBooking(booking);
        record.setCustomer(customer1);
        record.setVehicle(vehicle1);
        record.setServiceType(ServiceType.GENERAL_SERVICE);
        record.setServiceDate(LocalDate.now().minusDays(5));
        record.setMileage(16000);
        record.setServiceSummary("Full periodic service executed.");
        record.setActualBaseServiceAmount(new BigDecimal("1800.00"));
        record.setActualAdditionalRepairsAmount(BigDecimal.ZERO);
        record.setPickupDropCharge(BigDecimal.ZERO);
        record.setActualTotalAmount(new BigDecimal("1800.00"));
        record.setFinalizedAt(LocalDateTime.now().minusDays(5));

        ServiceItem item = new ServiceItem(record, "Engine Oil 5W30", "FLUIDS", 1, new BigDecimal("1200.00"), new BigDecimal("1200.00"));
        record.addItem(item);
        InspectionFinding finding = new InspectionFinding(record, "Brakes", "GOOD", "Pads in healthy condition.");
        record.addInspectionFinding(finding);
        record = serviceRecordRepository.save(record);

        String mockSummary = "Your Hyundai i20 received scheduled periodic maintenance including synthetic engine oil replacement. All brake and chassis inspection checkpoints were found in good condition.";
        when(geminiService.generateText(anyString(), anyString())).thenReturn(mockSummary);

        mockMvc.perform(get("/api/service-records/" + record.getId() + "/ai-summary")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceRecordId").value(record.getId()))
                .andExpect(jsonPath("$.summary", containsString("Hyundai i20")))
                .andExpect(jsonPath("$.disclaimer", containsString("AI-generated summary")));
    }

    @Test
    @DisplayName("TEST 6: AI Service Summary is rejected for unfinalized service records")
    void testAiServiceSummaryUnfinalizedRecord() throws Exception {
        ServiceBooking booking = new ServiceBooking(customer1, vehicle1, ServiceType.GENERAL_SERVICE, LocalDate.now(), TimeSlot.MORNING_SLOT_1, BookingStatus.CONFIRMED, false, 0, 1000, 1000);
        booking = bookingRepository.save(booking);

        ServiceRecord record = new ServiceRecord();
        record.setBooking(booking);
        record.setCustomer(customer1);
        record.setVehicle(vehicle1);
        record.setServiceType(ServiceType.GENERAL_SERVICE);
        record.setServiceDate(LocalDate.now());
        record.setMileage(15000);
        record.setServiceSummary("In progress...");
        record.setActualBaseServiceAmount(new BigDecimal("1000.00"));
        record.setActualTotalAmount(new BigDecimal("1000.00"));
        record.setFinalizedAt(null); // NOT finalized
        record = serviceRecordRepository.save(record);

        mockMvc.perform(get("/api/service-records/" + record.getId() + "/ai-summary")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("AI service summary is only available for completed and finalized service visits")));
    }

    @Test
    @DisplayName("TEST 7: Customer A cannot access AI summary for Customer B's service record (403 Forbidden)")
    void testAiServiceSummaryUnauthorizedRecord() throws Exception {
        ServiceBooking booking = new ServiceBooking(customer1, vehicle1, ServiceType.GENERAL_SERVICE, LocalDate.now().minusDays(2), TimeSlot.MORNING_SLOT_1, BookingStatus.CONFIRMED, false, 0, 1500, 1500);
        booking = bookingRepository.save(booking);

        ServiceRecord record = new ServiceRecord();
        record.setBooking(booking);
        record.setCustomer(customer1);
        record.setVehicle(vehicle1);
        record.setServiceType(ServiceType.GENERAL_SERVICE);
        record.setServiceDate(LocalDate.now().minusDays(2));
        record.setMileage(16000);
        record.setServiceSummary("Completed service");
        record.setActualBaseServiceAmount(new BigDecimal("1500.00"));
        record.setActualTotalAmount(new BigDecimal("1500.00"));
        record.setFinalizedAt(LocalDateTime.now().minusDays(2));
        record = serviceRecordRepository.save(record);

        // Customer 2 attempts to get AI summary for Customer 1's service record
        mockMvc.perform(get("/api/service-records/" + record.getId() + "/ai-summary")
                        .header("Authorization", "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 8: Service Center staff can generate AI summary for any finalized service record")
    void testAiServiceSummaryServiceCenterAllowed() throws Exception {
        ServiceBooking booking = new ServiceBooking(customer1, vehicle1, ServiceType.OIL_CHANGE, LocalDate.now().minusDays(3), TimeSlot.MORNING_SLOT_2, BookingStatus.CONFIRMED, false, 0, 1100, 1100);
        booking = bookingRepository.save(booking);

        ServiceRecord record = new ServiceRecord();
        record.setBooking(booking);
        record.setCustomer(customer1);
        record.setVehicle(vehicle1);
        record.setServiceType(ServiceType.OIL_CHANGE);
        record.setServiceDate(LocalDate.now().minusDays(3));
        record.setMileage(15500);
        record.setServiceSummary("Oil change performed");
        record.setActualBaseServiceAmount(new BigDecimal("1100.00"));
        record.setActualTotalAmount(new BigDecimal("1100.00"));
        record.setFinalizedAt(LocalDateTime.now().minusDays(3));
        record = serviceRecordRepository.save(record);

        when(geminiService.generateText(anyString(), anyString())).thenReturn("Service Center Summary: Oil change completed.");

        mockMvc.perform(get("/api/service-records/" + record.getId() + "/ai-summary")
                        .header("Authorization", "Bearer " + serviceCenterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary", containsString("Oil change completed")));
    }

    @Test
    @DisplayName("TEST 9: AI Service Summary handles Gemini API failure gracefully with 503 response")
    void testAiServiceSummaryGeminiFailureHandling() throws Exception {
        ServiceBooking booking = new ServiceBooking(customer1, vehicle1, ServiceType.OIL_CHANGE, LocalDate.now().minusDays(3), TimeSlot.MORNING_SLOT_2, BookingStatus.CONFIRMED, false, 0, 1100, 1100);
        booking = bookingRepository.save(booking);

        ServiceRecord record = new ServiceRecord();
        record.setBooking(booking);
        record.setCustomer(customer1);
        record.setVehicle(vehicle1);
        record.setServiceType(ServiceType.OIL_CHANGE);
        record.setServiceDate(LocalDate.now().minusDays(3));
        record.setMileage(15500);
        record.setServiceSummary("Oil change performed");
        record.setActualBaseServiceAmount(new BigDecimal("1100.00"));
        record.setActualTotalAmount(new BigDecimal("1100.00"));
        record.setFinalizedAt(LocalDateTime.now().minusDays(3));
        record = serviceRecordRepository.save(record);

        when(geminiService.generateText(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("AI service timed out. Please try again later."));

        mockMvc.perform(get("/api/service-records/" + record.getId() + "/ai-summary")
                        .header("Authorization", "Bearer " + customer1Token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message", containsString("AI service timed out")));
    }

    @Test
    @DisplayName("TEST 10: AI Service Advisor safely sanitizes unrecognized urgency to MEDIUM")
    void testAiAdvisorUrgencySanitization() throws Exception {
        String mockGeminiJsonWithWeirdUrgency = """
                {
                  "possibleSystem": "Electrical",
                  "recommendedService": "Battery Test",
                  "urgency": "EXTREME_DANGER",
                  "explanation": "Battery voltage seems low."
                }
                """;

        when(geminiService.generateJson(anyString(), anyString())).thenReturn(mockGeminiJsonWithWeirdUrgency);

        AiServiceAdvisorRequest request = new AiServiceAdvisorRequest(vehicle1.getId(), "Car takes multiple cranks to start in the morning.");

        mockMvc.perform(post("/api/ai/service-advisor")
                        .header("Authorization", "Bearer " + customer1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgency").value("MEDIUM"));
    }
}
