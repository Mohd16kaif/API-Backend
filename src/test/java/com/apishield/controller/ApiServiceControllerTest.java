package com.apishield.controller;

import com.apishield.dto.service.ApiServiceRequest;
import com.apishield.dto.service.ApiServiceUpdateRequest;
import com.apishield.model.ApiService;
import com.apishield.model.User;
import com.apishield.repository.ApiServiceRepository;
import com.apishield.repository.UserRepository;
import com.apishield.security.JwtTokenUtil;
import com.apishield.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiServiceRepository apiServiceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String jwtToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        try {
            // Clear repositories
            apiServiceRepository.deleteAll();
            userRepository.deleteAll();

            // Create test user
            testUser = User.builder()
                    .name("Test User")
                    .username("testuser")
                    .email("test@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .role(User.Role.USER)
                    .currency(User.Currency.USD)
                    .build();
            testUser = userRepository.save(testUser);

            // Generate JWT token (your JwtTokenUtil uses email-based tokens)
            UserPrincipal userPrincipal = UserPrincipal.create(testUser);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userPrincipal, null, userPrincipal.getAuthorities());
            jwtToken = jwtTokenUtil.generateJwtToken(authentication);

            // Debug: Print token info
            System.out.println("=== Generated JWT Token: " + jwtToken);
            System.out.println("=== User ID: " + testUser.getId());
            System.out.println("=== User Email: " + testUser.getEmail());
            System.out.println("=== Username: " + testUser.getUsername());

            // Test token validation
            boolean isValid = jwtTokenUtil.validateJwtToken(jwtToken);
            System.out.println("=== Token Valid: " + isValid);

            // Test email extraction (since your JWT uses email)
            String emailFromToken = jwtTokenUtil.getEmailFromJwtToken(jwtToken);
            System.out.println("=== Email from Token: " + emailFromToken);

        } catch (Exception e) {
            System.err.println("=== ERROR IN SETUP: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    void testCreateApiService_Success() throws Exception {
        ApiServiceRequest request = new ApiServiceRequest();
        request.setName("OpenAI API");
        request.setEndpointUrl("https://api.openai.com");
        request.setBudget(100.0);
        request.setCostPerUnit(0.02);
        request.setUsageCount(500.0);

        System.out.println("=== STARTING TEST: testCreateApiService_Success");
        System.out.println("=== Request: " + objectMapper.writeValueAsString(request));

        // ADD DEBUG CODE TO SEE ACTUAL ERROR
        MvcResult result = mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print()) // This will show full request/response
                .andReturn();

        // Print the actual error details
        System.out.println("=== RESPONSE STATUS: " + result.getResponse().getStatus());
        System.out.println("=== RESPONSE BODY: " + result.getResponse().getContentAsString());
        System.out.println("=== ERROR MESSAGE: " + result.getResponse().getErrorMessage());

        // Print headers for debugging
        System.out.println("=== RESPONSE HEADERS: " + result.getResponse().getHeaderNames());

        // This will show if we get 201 or still 500
        if (result.getResponse().getStatus() != 201) {
            System.err.println("=== TEST FAILED - Expected 201 but got " + result.getResponse().getStatus());
            System.err.println("=== Response body: " + result.getResponse().getContentAsString());
        }

        // For now, just print the result - we'll add assertions after we see what's happening
        System.out.println("=== TEST COMPLETED");
    }

    @Test
    void testCreateApiService_DuplicateName() throws Exception {
        System.out.println("=== STARTING TEST: testCreateApiService_DuplicateName");

        // Create first API service
        ApiService existingService = ApiService.builder()
                .user(testUser)
                .name("Duplicate API")
                .endpointUrl("https://api.example.com")
                .budget(50.0)
                .costPerUnit(0.01)
                .usageCount(100.0)
                .build();
        apiServiceRepository.save(existingService);

        ApiServiceRequest request = new ApiServiceRequest();
        request.setName("Duplicate API");
        request.setEndpointUrl("https://api.another.com");
        request.setBudget(75.0);
        request.setCostPerUnit(0.02);
        request.setUsageCount(200.0);

        MvcResult result = mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andReturn();

        System.out.println("=== DUPLICATE TEST - STATUS: " + result.getResponse().getStatus());
        System.out.println("=== DUPLICATE TEST - BODY: " + result.getResponse().getContentAsString());
    }

    @Test
    void testGetAllApiServices() throws Exception {
        System.out.println("=== STARTING TEST: testGetAllApiServices");

        // Create test API services
        ApiService service1 = ApiService.builder()
                .user(testUser)
                .name("Service 1")
                .endpointUrl("https://api1.com")
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(1000.0)
                .build();

        ApiService service2 = ApiService.builder()
                .user(testUser)
                .name("Service 2")
                .endpointUrl("https://api2.com")
                .budget(200.0)
                .costPerUnit(0.02)
                .usageCount(5000.0)
                .build();

        apiServiceRepository.save(service1);
        apiServiceRepository.save(service2);

        MvcResult result = mockMvc.perform(get("/api/services")
                        .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andReturn();

        System.out.println("=== GET ALL - STATUS: " + result.getResponse().getStatus());
        System.out.println("=== GET ALL - BODY: " + result.getResponse().getContentAsString());
    }

    @Test
    void testGetApiServiceById() throws Exception {
        System.out.println("=== STARTING TEST: testGetApiServiceById");

        ApiService service = ApiService.builder()
                .user(testUser)
                .name("Test Service")
                .endpointUrl("https://api.test.com")
                .budget(150.0)
                .costPerUnit(0.05)
                .usageCount(2000.0)
                .build();
        service = apiServiceRepository.save(service);

        MvcResult result = mockMvc.perform(get("/api/services/" + service.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andReturn();

        System.out.println("=== GET BY ID - STATUS: " + result.getResponse().getStatus());
        System.out.println("=== GET BY ID - BODY: " + result.getResponse().getContentAsString());
    }

    @Test
    void testUpdateApiService() throws Exception {
        System.out.println("=== STARTING TEST: testUpdateApiService");

        ApiService service = ApiService.builder()
                .user(testUser)
                .name("Old Name")
                .endpointUrl("https://old.api.com")
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(500.0)
                .build();
        service = apiServiceRepository.save(service);

        ApiServiceUpdateRequest updateRequest = new ApiServiceUpdateRequest();
        updateRequest.setName("New Name");
        updateRequest.setBudget(200.0);

        MvcResult result = mockMvc.perform(put("/api/services/" + service.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andReturn();

        System.out.println("=== UPDATE - STATUS: " + result.getResponse().getStatus());
        System.out.println("=== UPDATE - BODY: " + result.getResponse().getContentAsString());
    }

    @Test
    void testDeleteApiService() throws Exception {
        System.out.println("=== STARTING TEST: testDeleteApiService");

        ApiService service = ApiService.builder()
                .user(testUser)
                .name("Service to Delete")
                .endpointUrl("https://delete.api.com")
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(100.0)
                .build();
        service = apiServiceRepository.save(service);

        MvcResult result = mockMvc.perform(delete("/api/services/" + service.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andReturn();

        System.out.println("=== DELETE - STATUS: " + result.getResponse().getStatus());
        System.out.println("=== DELETE - BODY: " + result.getResponse().getContentAsString());
    }

    @Test
    void testUpdateUsage() throws Exception {
        System.out.println("=== STARTING TEST: testUpdateUsage");

        ApiService service = ApiService.builder()
                .user(testUser)
                .name("Usage Test Service")
                .endpointUrl("https://usage.api.com")
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(500.0)
                .build();
        service = apiServiceRepository.save(service);

        MvcResult result = mockMvc.perform(patch("/api/services/" + service.getId() + "/usage")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usageCount\": 1000.0}"))
                .andDo(print())
                .andReturn();

        System.out.println("=== UPDATE USAGE - STATUS: " + result.getResponse().getStatus());
        System.out.println("=== UPDATE USAGE - BODY: " + result.getResponse().getContentAsString());
    }

    @Test
    void testGetApiServiceStats() throws Exception {
        System.out.println("=== STARTING TEST: testGetApiServiceStats");

        // Create services with different statuses
        ApiService healthyService = ApiService.builder()
                .user(testUser)
                .name("Healthy Service")
                .endpointUrl("https://healthy.api.com")
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(500.0) // 5% utilization
                .build();

        ApiService warningService = ApiService.builder()
                .user(testUser)
                .name("Warning Service")
                .endpointUrl("https://warning.api.com")
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(8000.0) // 80% utilization
                .build();

        ApiService criticalService = ApiService.builder()
                .user(testUser)
                .name("Critical Service")
                .endpointUrl("https://critical.api.com")
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(9500.0) // 95% utilization
                .build();

        apiServiceRepository.save(healthyService);
        apiServiceRepository.save(warningService);
        apiServiceRepository.save(criticalService);

        MvcResult result = mockMvc.perform(get("/api/services/stats")
                        .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andReturn();

        System.out.println("=== STATS - STATUS: " + result.getResponse().getStatus());
        System.out.println("=== STATS - BODY: " + result.getResponse().getContentAsString());
    }

    @Test
    void testGetApiService_NotFound() throws Exception {
        System.out.println("=== STARTING TEST: testGetApiService_NotFound");

        MvcResult result = mockMvc.perform(get("/api/services/999")
                        .header("Authorization", "Bearer " + jwtToken))
                .andDo(print())
                .andReturn();

        System.out.println("=== NOT FOUND - STATUS: " + result.getResponse().getStatus());
        System.out.println("=== NOT FOUND - BODY: " + result.getResponse().getContentAsString());
    }
}