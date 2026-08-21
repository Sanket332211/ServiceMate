package com.example.carservice;

import com.example.carservice.dto.VehicleRequest;
import com.example.carservice.entity.FuelType;
import com.example.carservice.entity.Role;
import com.example.carservice.entity.Transmission;
import com.example.carservice.entity.User;
import com.example.carservice.entity.Vehicle;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class VehicleControllerTest {

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
    private String tokenCustomer1;
    private String tokenCustomer2;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        repairRepository.deleteAll();
        workflowRepository.deleteAll();
        bookingRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();

        // Create Customer 1 (Rahul)
        customer1 = new User("Rahul", "rahul@example.com", "9876543210", passwordEncoder.encode("password123"), Role.CUSTOMER);
        userRepository.save(customer1);
        tokenCustomer1 = jwtService.generateToken(customer1);

        // Create Customer 2 (Sneha)
        customer2 = new User("Sneha", "sneha@example.com", "9123456780", passwordEncoder.encode("password123"), Role.CUSTOMER);
        userRepository.save(customer2);
        tokenCustomer2 = jwtService.generateToken(customer2);
    }

    @Test
    @DisplayName("TEST 1: Customer creates vehicle successfully with uppercase normalized plate")
    void testCreateVehicleSuccess() throws Exception {
        VehicleRequest request = new VehicleRequest(
                "mh12ab1234", "Honda", "City", 2021, FuelType.PETROL, Transmission.AUTOMATIC, 25000
        );

        mockMvc.perform(post("/api/vehicles")
                .header("Authorization", "Bearer " + tokenCustomer1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.registrationNumber", is("MH12AB1234")))
                .andExpect(jsonPath("$.make", is("Honda")))
                .andExpect(jsonPath("$.model", is("City")))
                .andExpect(jsonPath("$.manufacturingYear", is(2021)))
                .andExpect(jsonPath("$.fuelType", is("PETROL")))
                .andExpect(jsonPath("$.transmission", is("AUTOMATIC")))
                .andExpect(jsonPath("$.currentMileage", is(25000)))
                .andExpect(jsonPath("$.ownerEmail", is("rahul@example.com")));

        // Verify in DB
        Vehicle saved = vehicleRepository.findByRegistrationNumber("MH12AB1234").orElseThrow();
        assertEquals(customer1.getId(), saved.getOwner().getId());
    }

    @Test
    @DisplayName("TEST 2: Duplicate registration number is rejected with 400 Bad Request")
    void testDuplicateRegistrationNumber() throws Exception {
        // First vehicle
        Vehicle v1 = new Vehicle(customer1, "MH12AB1234", "Honda", "City", 2021, FuelType.PETROL, Transmission.AUTOMATIC, 25000);
        vehicleRepository.save(v1);

        // Attempting to add duplicate plate (even with lowercase)
        VehicleRequest duplicateRequest = new VehicleRequest(
                "mh12ab1234", "Hyundai", "i20", 2022, FuelType.DIESEL, Transmission.MANUAL, 12000
        );

        mockMvc.perform(post("/api/vehicles")
                .header("Authorization", "Bearer " + tokenCustomer1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("TEST 3: Customer views only their own vehicles (Data Isolation)")
    void testGetMyVehiclesDataIsolation() throws Exception {
        // Customer 1 car
        Vehicle v1 = new Vehicle(customer1, "MH12AB1234", "Honda", "City", 2021, FuelType.PETROL, Transmission.AUTOMATIC, 25000);
        vehicleRepository.save(v1);

        // Customer 2 car
        Vehicle v2 = new Vehicle(customer2, "KA01XY9999", "Tata", "Nexon EV", 2023, FuelType.ELECTRIC, Transmission.AUTOMATIC, 8000);
        vehicleRepository.save(v2);

        // Customer 1 fetches vehicles -> should see only 1 vehicle (MH12AB1234)
        mockMvc.perform(get("/api/vehicles")
                .header("Authorization", "Bearer " + tokenCustomer1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].registrationNumber", is("MH12AB1234")));

        // Customer 2 fetches vehicles -> should see only 1 vehicle (KA01XY9999)
        mockMvc.perform(get("/api/vehicles")
                .header("Authorization", "Bearer " + tokenCustomer2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].registrationNumber", is("KA01XY9999")));
    }

    @Test
    @DisplayName("TEST 4: Customer updates their own vehicle successfully")
    void testUpdateVehicleSuccess() throws Exception {
        Vehicle v1 = new Vehicle(customer1, "MH12AB1234", "Honda", "City", 2021, FuelType.PETROL, Transmission.AUTOMATIC, 25000);
        Vehicle saved = vehicleRepository.save(v1);

        VehicleRequest updateReq = new VehicleRequest(
                "MH12AB1234", "Honda", "City ZX", 2021, FuelType.PETROL, Transmission.AUTOMATIC, 28000
        );

        mockMvc.perform(put("/api/vehicles/" + saved.getId())
                .header("Authorization", "Bearer " + tokenCustomer1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model", is("City ZX")))
                .andExpect(jsonPath("$.currentMileage", is(28000)));
    }

    @Test
    @DisplayName("TEST 5: Customer A cannot update Customer B's vehicle (403 Forbidden)")
    void testCustomerCannotUpdateOtherCustomerVehicle() throws Exception {
        Vehicle v2 = new Vehicle(customer2, "KA01XY9999", "Tata", "Nexon EV", 2023, FuelType.ELECTRIC, Transmission.AUTOMATIC, 8000);
        Vehicle saved = vehicleRepository.save(v2);

        VehicleRequest maliciousUpdate = new VehicleRequest(
                "KA01XY9999", "Tata", "Hacked", 2023, FuelType.ELECTRIC, Transmission.AUTOMATIC, 90000
        );

        // Customer 1 tries to update Customer 2's car
        mockMvc.perform(put("/api/vehicles/" + saved.getId())
                .header("Authorization", "Bearer " + tokenCustomer1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(maliciousUpdate)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 6: Customer deletes their vehicle successfully")
    void testDeleteVehicleSuccess() throws Exception {
        Vehicle v1 = new Vehicle(customer1, "MH12AB1234", "Honda", "City", 2021, FuelType.PETROL, Transmission.AUTOMATIC, 25000);
        Vehicle saved = vehicleRepository.save(v1);

        mockMvc.perform(delete("/api/vehicles/" + saved.getId())
                .header("Authorization", "Bearer " + tokenCustomer1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        assertFalse(vehicleRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("TEST 7: Customer A cannot delete Customer B's vehicle (403 Forbidden)")
    void testCustomerCannotDeleteOtherCustomerVehicle() throws Exception {
        Vehicle v2 = new Vehicle(customer2, "KA01XY9999", "Tata", "Nexon EV", 2023, FuelType.ELECTRIC, Transmission.AUTOMATIC, 8000);
        Vehicle saved = vehicleRepository.save(v2);

        // Customer 1 tries to delete Customer 2's car
        mockMvc.perform(delete("/api/vehicles/" + saved.getId())
                .header("Authorization", "Bearer " + tokenCustomer1))
                .andExpect(status().isForbidden());

        assertTrue(vehicleRepository.existsById(saved.getId()));
    }

    @Test
    @DisplayName("TEST 8: Unauthenticated access to /api/vehicles returns 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized());
    }
}
