package com.apishield.controller;

import com.apishield.dto.plan.SubscribeRequest;
import com.apishield.model.*;
import com.apishield.repository.*;
import com.apishield.security.JwtTokenUtil;
import com.apishield.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private UserSubscriptionRepository subscriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String jwtToken;
    private User testUser;
    private SubscriptionPlan starterPlan;
    private SubscriptionPlan proPlan;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        planRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .name("Plan Test User")
                .username("plantestuser")  // Added username field
                .email("plan@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .currency(User.Currency.USD)
                .build();
        testUser = userRepository.save(testUser);

        // Create test plans
        starterPlan = SubscriptionPlan.builder()
                .name("Starter")
                .priceUsd(0.0)
                .priceInr(0.0)
                .features("Basic features,5 API services,Email support")
                .maxApis(5)
                .maxRequestsPerMonth(10000L)
                .supportLevel(SubscriptionPlan.SupportLevel.BASIC)
                .isActive(true)
                .displayOrder(1)
                .build();
        starterPlan = planRepository.save(starterPlan);

        proPlan = SubscriptionPlan.builder()
                .name("Pro")
                .priceUsd(25.0)
                .priceInr(2075.0)
                .features("Advanced features,20 API services,Priority support")
                .maxApis(20)
                .maxRequestsPerMonth(100000L)
                .supportLevel(SubscriptionPlan.SupportLevel.PRIORITY)
                .isActive(true)
                .displayOrder(2)
                .build();
        proPlan = planRepository.save(proPlan);

        // Fixed: Create authenticated TestingAuthenticationToken
        UserPrincipal userPrincipal = UserPrincipal.create(testUser);
        Authentication authentication = new TestingAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );
        jwtToken = jwtTokenUtil.generateJwtToken(authentication);
    }

    @Test
    void testGetAllPlans() throws Exception {
        mockMvc.perform(get("/api/plans")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Starter"))
                .andExpect(jsonPath("$[0].priceUsd").value(0.0))
                .andExpect(jsonPath("$[0].isFreePlan").value(true))
                .andExpect(jsonPath("$[0].formattedPrice").value("$0.00/month"))
                .andExpect(jsonPath("$[1].name").value("Pro"))
                .andExpect(jsonPath("$[1].priceUsd").value(25.0))
                .andExpect(jsonPath("$[1].isFreePlan").value(false));
    }

    @Test
    void testGetPlanById() throws Exception {
        mockMvc.perform(get("/api/plans/" + proPlan.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pro"))
                .andExpect(jsonPath("$.maxApis").value(20))
                .andExpect(jsonPath("$.maxRequestsPerMonth").value(100000))
                .andExpect(jsonPath("$.supportLevel").value("PRIORITY"));
    }

    @Test
    void testSubscribeToFreePlan() throws Exception {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(starterPlan.getId());
        request.setPaymentMode(UserSubscription.PaymentMode.UPI);
        request.setAutoRenew(false);

        mockMvc.perform(post("/api/plans/subscribe")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planName").value("Starter"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.paymentReference").value(startsWith("FREE_PLAN_")));
    }

    @Test
    void testSubscribeToPaidPlan() throws Exception {
        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(proPlan.getId());
        request.setPaymentMode(UserSubscription.PaymentMode.PAYPAL);
        request.setAutoRenew(true);
        // Remove the non-existent method calls
        // The controller might need these fields, check SubscribeRequest DTO for available setters

        mockMvc.perform(post("/api/plans/subscribe")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Let's first see what the actual response is by expecting a range of possible statuses
                .andExpect(status().isCreated()) // Keep this for now, but we might need to adjust
                .andExpect(jsonPath("$.planName").value("Pro"))
                .andExpect(jsonPath("$.paymentMode").value("PAYPAL"))
                .andExpect(jsonPath("$.autoRenew").value(true))
                .andExpect(jsonPath("$.formattedAmountPaid").value("$25.00"));
    }

    @Test
    void testGetCurrentSubscription() throws Exception {
        // First subscribe to a plan
        UserSubscription subscription = UserSubscription.builder()
                .user(testUser)
                .plan(starterPlan)
                .paymentMode(UserSubscription.PaymentMode.UPI)
                .status(UserSubscription.Status.ACTIVE)
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusMonths(1))
                .amountPaid(0.0)
                .currencyPaid(User.Currency.USD)
                .paymentReference("FREE_PLAN_123")
                .autoRenew(false)
                .build();
        subscriptionRepository.save(subscription);

        mockMvc.perform(get("/api/plans/current")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planName").value("Starter"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.daysRemaining").value(greaterThan(25)));
    }

    @Test
    void testGetCurrentSubscription_NoSubscription() throws Exception {
        mockMvc.perform(get("/api/plans/current")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void testCancelSubscription() throws Exception {
        // Create active subscription
        UserSubscription subscription = UserSubscription.builder()
                .user(testUser)
                .plan(proPlan)
                .paymentMode(UserSubscription.PaymentMode.PAYPAL)
                .status(UserSubscription.Status.ACTIVE)
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusMonths(1))
                .amountPaid(25.0)
                .currencyPaid(User.Currency.USD)
                .paymentReference("PP_123456789")
                .autoRenew(true)
                .build();
        subscriptionRepository.save(subscription);

        mockMvc.perform(post("/api/plans/cancel")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"No longer needed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.autoRenew").value(false));
    }

    @Test
    void testGetBillingHistory() throws Exception {
        // Create multiple subscriptions
        UserSubscription oldSubscription = UserSubscription.builder()
                .user(testUser)
                .plan(starterPlan)
                .paymentMode(UserSubscription.PaymentMode.UPI)
                .status(UserSubscription.Status.EXPIRED)
                .startDate(java.time.LocalDate.now().minusMonths(2))
                .endDate(java.time.LocalDate.now().minusMonths(1))
                .amountPaid(0.0)
                .currencyPaid(User.Currency.USD)
                .build();

        UserSubscription currentSubscription = UserSubscription.builder()
                .user(testUser)
                .plan(proPlan)
                .paymentMode(UserSubscription.PaymentMode.PAYPAL)
                .status(UserSubscription.Status.ACTIVE)
                .startDate(java.time.LocalDate.now())
                .endDate(java.time.LocalDate.now().plusMonths(1))
                .amountPaid(25.0)
                .currencyPaid(User.Currency.USD)
                .build();

        subscriptionRepository.save(oldSubscription);
        subscriptionRepository.save(currentSubscription);

        mockMvc.perform(get("/api/plans/billing/history")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscriptions", hasSize(2)))
                .andExpect(jsonPath("$.totalAmountPaid").value(25.0))
                .andExpect(jsonPath("$.totalAmountFormatted").value("$25.00"))
                .andExpect(jsonPath("$.totalSubscriptions").value(2))
                .andExpect(jsonPath("$.currentPlan").value("Pro"))
                .andExpect(jsonPath("$.hasActiveSubscription").value(true))
                .andExpect(jsonPath("$.stats.mostUsedPaymentMode").exists());
    }

    @Test
    void testGetPaymentMethods() throws Exception {
        mockMvc.perform(get("/api/plans/payment-methods")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("paymentMode", "PAYPAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("PayPal"))
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.supported_currencies").isArray())
                .andExpect(jsonPath("$.processing_time").exists());
    }

    @Test
    void testCheckPaymentMethodSupport() throws Exception {
        mockMvc.perform(get("/api/plans/payment-methods/supported")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("paymentMode", "UPI")
                        .param("currency", "INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supported").value(true));

        mockMvc.perform(get("/api/plans/payment-methods/supported")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("paymentMode", "UPI")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supported").value(false));
    }

    @Test
    void testCalculateProcessingFee() throws Exception {
        mockMvc.perform(get("/api/plans/payment-methods/fee")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("paymentMode", "PAYPAL")
                        .param("amount", "100.0")
                        .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.processing_fee").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.total").value(greaterThan(100.0)));
    }

    @Test
    void testGetPaymentInstructions() throws Exception {
        mockMvc.perform(get("/api/plans/payment-instructions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .param("paymentMode", "UPI")
                        .param("amount", "100.0")
                        .param("currency", "INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructions").isArray())
                .andExpect(jsonPath("$.upi_id").exists())
                .andExpect(jsonPath("$.qr_code_url").exists());
    }

    @Test
    void testSubscribeToInactivePlan() throws Exception {
        // Deactivate plan
        starterPlan.setIsActive(false);
        planRepository.save(starterPlan);

        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(starterPlan.getId());
        request.setPaymentMode(UserSubscription.PaymentMode.UPI);

        mockMvc.perform(post("/api/plans/subscribe")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("no longer available")));
    }
}