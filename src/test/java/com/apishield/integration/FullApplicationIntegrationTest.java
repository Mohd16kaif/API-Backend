package com.apishield.integration;

import com.apishield.dto.auth.LoginRequest;
import com.apishield.dto.auth.RegisterRequest;
import com.apishield.dto.service.ApiServiceRequest;
import com.apishield.dto.plan.SubscribeRequest;
import com.apishield.model.User;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import com.apishield.model.UserSubscription;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class FullApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Sql("/test-data/plans.sql")
    void testCompleteUserJourney() throws Exception {
        // 1. Register a new user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Integration Test User");
        registerRequest.setEmail("integration@test.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.name").value("Integration Test User"))
                .andExpect(jsonPath("$.user.email").value("integration@test.com"));

        // 2. Login to get JWT token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("integration@test.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String loginResponseJson = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponseJson).get("token").asText();

        // 3. Create an API service
        ApiServiceRequest serviceRequest = new ApiServiceRequest();
        serviceRequest.setName("Test API Service");
        serviceRequest.setEndpointUrl("https://api.test.com");
        serviceRequest.setBudget(1000.0);
        serviceRequest.setCostPerUnit(0.01);

        MvcResult serviceResult = mockMvc.perform(post("/api/services")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(serviceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test API Service"))
                .andExpect(jsonPath("$.budget").value(1000.0))
                .andReturn();

        String serviceResponseJson = serviceResult.getResponse().getContentAsString();
        Long serviceId = objectMapper.readTree(serviceResponseJson).get("id").asLong();

        // 4. Get user settings
        mockMvc.perform(get("/api/user/settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyPreference").value("USD"))
                .andExpect(jsonPath("$.theme").value("LIGHT"));

        // 5. Update service usage
        Map<String, Double> usageRequest = Map.of("usageCount", 500.0);
        mockMvc.perform(patch("/api/services/" + serviceId + "/usage")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usageRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usageCount").value(500.0))
                .andExpect(jsonPath("$.utilizationPercentage").value(0.5));

        // 6. Get usage analytics
        mockMvc.perform(get("/api/services/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalServices").value(1));

        // 7. Get subscription plans - NOW WITH DEBUG INFO
        MvcResult plansResult = mockMvc.perform(get("/api/plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        // Debug: Print what we actually got
        String plansResponse = plansResult.getResponse().getContentAsString();
        System.out.println("Plans response: " + plansResponse);

        // Now test that we have plans
        mockMvc.perform(get("/api/plans")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name").exists());

        // 8. Subscribe to a plan (free starter plan)
        SubscribeRequest subscribeRequest = new SubscribeRequest();
        subscribeRequest.setPlanId(1L); // Using the Starter plan we inserted
        subscribeRequest.setPaymentMode(UserSubscription.PaymentMode.UPI);
        subscribeRequest.setAutoRenew(false);

        mockMvc.perform(post("/api/plans/subscribe")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subscribeRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 9. Check current subscription
        mockMvc.perform(get("/api/plans/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 10. Get all services (should show the created service)
        mockMvc.perform(get("/api/services")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Test API Service"));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        // Test accessing protected endpoints without token
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/user/settings"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/plans/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testInvalidTokenAccess() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(get("/api/services")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testHealthEndpoints() throws Exception {
        // Test health endpoint - let's see what it actually returns
        MvcResult result = mockMvc.perform(get("/actuator/health"))
                .andReturn();

        System.out.println("Health endpoint status: " + result.getResponse().getStatus());
        System.out.println("Health endpoint response: " + result.getResponse().getContentAsString());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Disabled("SpringDoc compatibility issue - temporarily disabled")
    void testSwaggerEndpoints() throws Exception {
        // Test OpenAPI docs
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.0.1"));

        // Test Swagger UI (should redirect)
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}