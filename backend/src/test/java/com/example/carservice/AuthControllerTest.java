package com.example.carservice;

import com.example.carservice.dto.LoginRequest;
import com.example.carservice.dto.RegisterRequest;
import com.example.carservice.entity.Role;
import com.example.carservice.entity.User;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class AuthControllerTest {

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
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        repairRepository.deleteAll();
        workflowRepository.deleteAll();
        bookingRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("TEST 1: Customer Registration succeeds, assigns CUSTOMER role, and hashes password")
    void testCustomerRegistrationSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("Rahul", "rahul@example.com", "9876543210", "password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email", is("rahul@example.com")))
                .andExpect(jsonPath("$.name", is("Rahul")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")));

        // Verify password in DB is BCrypt hashed, NOT plain text
        User savedUser = userRepository.findByEmail("rahul@example.com").orElseThrow();
        assertNotEquals("password123", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()));
        assertEquals(Role.CUSTOMER, savedUser.getRole());
    }

    @Test
    @DisplayName("TEST 2: Duplicate Registration is rejected with 400 Bad Request")
    void testDuplicateRegistration() throws Exception {
        // First registration
        RegisterRequest request1 = new RegisterRequest("Rahul", "rahul@example.com", "9876543210", "password123");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Duplicate registration attempt
        RegisterRequest request2 = new RegisterRequest("Rahul Duplicate", "rahul@example.com", "1234567890", "differentPass");
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("TEST 3: Login with correct credentials returns JWT and user info")
    void testLoginSuccess() throws Exception {
        // Register user
        User user = new User("Rahul", "rahul@example.com", "9876543210", passwordEncoder.encode("password123"), Role.CUSTOMER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("rahul@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email", is("rahul@example.com")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")));
    }

    @Test
    @DisplayName("TEST 4: Wrong password returns 401 Unauthorized")
    void testLoginWrongPassword() throws Exception {
        User user = new User("Rahul", "rahul@example.com", "9876543210", passwordEncoder.encode("password123"), Role.CUSTOMER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("rahul@example.com", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid email or password.")));
    }

    @Test
    @DisplayName("TEST 5: CUSTOMER token accessing SERVICE_CENTER endpoint receives 403 Forbidden")
    void testCustomerForbiddenFromServiceCenterEndpoint() throws Exception {
        // Create Customer and get token
        RegisterRequest registerRequest = new RegisterRequest("Rahul", "rahul@example.com", "9876543210", "password123");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = regResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("token").asText();

        // Customer accesses customer test endpoint -> 200 OK
        mockMvc.perform(get("/api/auth/customer-test")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Customer attempts to access service center test endpoint -> 403 Forbidden
        mockMvc.perform(get("/api/auth/service-center-test")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("TEST 6: SERVICE_CENTER token accessing SERVICE_CENTER endpoint succeeds")
    void testServiceCenterAccessAllowed() throws Exception {
        // Create Admin user with SERVICE_CENTER role
        User admin = new User("Service Admin", "admin@servicemate.com", "9998887770", passwordEncoder.encode("admin123"), Role.SERVICE_CENTER);
        userRepository.save(admin);

        LoginRequest loginRequest = new LoginRequest("admin@servicemate.com", "admin123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("token").asText();

        // Access Service Center protected endpoint -> 200 OK
        mockMvc.perform(get("/api/auth/service-center-test")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("admin@servicemate.com")));
    }

    @Test
    @DisplayName("TEST 7: No JWT on protected endpoint returns 401 Unauthorized")
    void testProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/customer-test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("TEST 8: Invalid JWT on protected endpoint returns 401 Unauthorized")
    void testProtectedEndpointWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/customer-test")
                .header("Authorization", "Bearer invalid.fake.token123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("TEST 9: CORS preflight (OPTIONS) request to /api/auth/login succeeds with 200 and required CORS headers")
    void testCorsPreflightOnLogin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                .header("Origin", "https://service-mate-one.vercel.app")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://service-mate-one.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    @DisplayName("TEST 10: Failed login returns 401 Unauthorized with explicit CORS headers")
    void testFailedLoginIncludesCorsHeaders() throws Exception {
        User user = new User("Rahul", "rahul@example.com", "9876543210", passwordEncoder.encode("password123"), Role.CUSTOMER);
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("rahul@example.com", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                .header("Origin", "https://service-mate-one.vercel.app")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://service-mate-one.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)));
    }

    @Test
    @DisplayName("TEST 11: Validation error returns 400 Bad Request with explicit CORS headers")
    void testValidationErrorIncludesCorsHeaders() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("invalid-email-format", "");
        mockMvc.perform(post("/api/auth/login")
                .header("Origin", "https://service-mate-one.vercel.app")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://service-mate-one.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("TEST 12: Security rejection on unauthenticated endpoint returns 401 with CORS headers")
    void testSecurityUnauthorizedIncludesCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/auth/customer-test")
                .header("Origin", "https://service-mate-one.vercel.app"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://service-mate-one.vercel.app"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.status", is(401)));
    }
}
