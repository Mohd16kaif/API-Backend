package com.apishield.controller;

import com.apishield.dto.budget.BudgetRequest;
import com.apishield.dto.budget.BudgetUpdateRequest;
import com.apishield.model.ApiService;
import com.apishield.model.Budget;
import com.apishield.model.User;
import com.apishield.repository.ApiServiceRepository;
import com.apishield.repository.BudgetRepository;
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
class BudgetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BudgetRepository budgetRepository;

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
        budgetRepository.deleteAll();
        apiServiceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .name("Budget Test User")
                .username("budgettestuser")
                .email("budget@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .currency(User.Currency.USD)
                .build();
        testUser = userRepository.save(testUser);

        UserPrincipal userPrincipal = UserPrincipal.create(testUser);
        Authentication authentication = new org.springframework.security.authentication.TestingAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());
        jwtToken = jwtTokenUtil.generateJwtToken(authentication);
    }

    @Test
    void testCreateBudget_Success() throws Exception {
        BudgetRequest request = new BudgetRequest();
        request.setMonthlyBudget(1000.0);

        mockMvc.perform(post("/api/budget")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyBudget").value(1000.0))
                .andExpect(jsonPath("$.spentAmount").value(0.0))
                .andExpect(jsonPath("$.remainingBudget").value(1000.0))
                .andExpect(jsonPath("$.utilizationPercentage").value(0.0))
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.daysLeftInMonth").isNumber())
                .andExpect(jsonPath("$.currencySymbol").value("$"));
    }

    @Test
    void testGetBudget_WithApiServices() throws Exception {
        // Create budget
        Budget budget = Budget.builder()
                .user(testUser)
                .monthlyBudget(500.0)
                .spentAmount(0.0)
                .build();
        budgetRepository.save(budget);

        // Create API services with usage
        ApiService service1 = ApiService.builder()
                .user(testUser)
                .name("Service 1")
                .endpointUrl("https://api1.com")
                .budget(200.0)
                .costPerUnit(0.10)
                .usageCount(1000.0) // $100 spent
                .build();

        ApiService service2 = ApiService.builder()
                .user(testUser)
                .name("Service 2")
                .endpointUrl("https://api2.com")
                .budget(300.0)
                .costPerUnit(0.05)
                .usageCount(3000.0) // $150 spent
                .build();

        apiServiceRepository.save(service1);
        apiServiceRepository.save(service2);

        mockMvc.perform(get("/api/budget")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyBudget").value(500.0))
                .andExpect(jsonPath("$.spentAmount").value(250.0)) // $100 + $150
                .andExpect(jsonPath("$.remainingBudget").value(250.0))
                .andExpect(jsonPath("$.utilizationPercentage").value(50.0))
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.totalApiServices").value(2))
                .andExpect(jsonPath("$.averageSpendPerService").value(125.0));
    }

    @Test
    void testUpdateBudget_Success() throws Exception {
        // Create existing budget
        Budget budget = Budget.builder()
                .user(testUser)
                .monthlyBudget(500.0)
                .spentAmount(100.0)
                .build();
        budgetRepository.save(budget);

        BudgetUpdateRequest request = new BudgetUpdateRequest();
        request.setMonthlyBudget(800.0);

        mockMvc.perform(put("/api/budget")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyBudget").value(800.0))
                .andExpect(jsonPath("$.utilizationPercentage").value(lessThan(50.0))); // Should be recalculated
    }

    @Test
    void testGetBudgetStatus() throws Exception {
        // Create budget
        Budget budget = Budget.builder()
                .user(testUser)
                .monthlyBudget(100.0)
                .spentAmount(0.0) // Will be calculated from API services
                .build();
        budgetRepository.save(budget);

        // Create API service with high utilization to achieve 95% spending
        ApiService service = ApiService.builder()
                .user(testUser)
                .name("High Usage Service")
                .endpointUrl("https://highusage.api.com")
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(9500.0) // $95 spent (95% utilization)
                .build();
        apiServiceRepository.save(service);

        mockMvc.perform(get("/api/budget/status")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasBudget").value(true))
                .andExpect(jsonPath("$.isOverBudget").value(false))
                .andExpect(jsonPath("$.utilizationPercentage").value(95.0))
                .andExpect(jsonPath("$.status").value("critical"))
                .andExpect(jsonPath("$.daysLeftInMonth").isNumber());
    }

    @Test
    void testGetBudgetInsights() throws Exception {
        // Create budget
        Budget budget = Budget.builder()
                .user(testUser)
                .monthlyBudget(1000.0)
                .spentAmount(300.0)
                .build();
        budgetRepository.save(budget);

        mockMvc.perform(get("/api/budget/insights")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendation").isString())
                .andExpect(jsonPath("$.spendingTrend").isString())
                .andExpect(jsonPath("$.dailyBudgetRemaining").isNumber())
                .andExpect(jsonPath("$.spendingVelocity").isNumber())
                .andExpect(jsonPath("$.budgetHealthScore").isNumber());
    }

    @Test
    void testRefreshBudget() throws Exception {
        // Create budget
        Budget budget = Budget.builder()
                .user(testUser)
                .monthlyBudget(500.0)
                .spentAmount(50.0) // Outdated amount
                .build();
        budgetRepository.save(budget);

        // Create API service with different spending
        ApiService service = ApiService.builder()
                .user(testUser)
                .name("Test Service")
                .endpointUrl("https://test.api.com")
                .budget(200.0)
                .costPerUnit(0.20)
                .usageCount(500.0) // $100 actual spent
                .build();
        apiServiceRepository.save(service);

        mockMvc.perform(post("/api/budget/refresh")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentAmount").value(100.0)) // Should be refreshed
                .andExpect(jsonPath("$.utilizationPercentage").value(20.0));
    }

    @Test
    void testCreateBudget_InvalidAmount() throws Exception {
        BudgetRequest request = new BudgetRequest();
        request.setMonthlyBudget(-100.0); // Invalid negative amount

        mockMvc.perform(post("/api/budget")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    void testGetBudget_NoBudgetExists() throws Exception {
        // No budget created for user
        mockMvc.perform(get("/api/budget")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyBudget").value(0.0))
                .andExpect(jsonPath("$.spentAmount").value(0.0))
                .andExpect(jsonPath("$.status").value("healthy"));
    }

    @Test
    void testBudgetWithOverBudgetScenario() throws Exception {
        // Create budget
        Budget budget = Budget.builder()
                .user(testUser)
                .monthlyBudget(100.0)
                .spentAmount(0.0)
                .build();
        budgetRepository.save(budget);

        // Create API service that exceeds budget
        ApiService service = ApiService.builder()
                .user(testUser)
                .name("Expensive Service")
                .endpointUrl("https://expensive.api.com")
                .budget(50.0)
                .costPerUnit(1.0)
                .usageCount(150.0) // $150 spent, over $100 budget
                .build();
        apiServiceRepository.save(service);

        mockMvc.perform(get("/api/budget")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentAmount").value(150.0))
                .andExpect(jsonPath("$.remainingBudget").value(0.0))
                .andExpect(jsonPath("$.utilizationPercentage").value(100.0))
                .andExpect(jsonPath("$.status").value("over_budget"));
        // Removed the isOverBudget check since it's not available in this endpoint
    }
}