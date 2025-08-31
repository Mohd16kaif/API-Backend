package com.apishield.controller;

import com.apishield.dto.usage.UsageLogRequest;
import com.apishield.model.ApiService;
import com.apishield.model.User;
import com.apishield.model.UsageLog;
import com.apishield.repository.ApiServiceRepository;
import com.apishield.repository.UsageLogRepository;
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

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UsageLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiServiceRepository apiServiceRepository;

    @Autowired
    private UsageLogRepository usageLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String jwtToken;
    private User testUser;
    private ApiService testApiService;

    @BeforeEach
    void setUp() {
        usageLogRepository.deleteAll();
        apiServiceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .name("Usage Test User")
                .email("usage@example.com")
                .username("usagetest_" + System.currentTimeMillis()) // More unique username
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .currency(User.Currency.USD)
                .build();
        testUser = userRepository.save(testUser);

        testApiService = ApiService.builder()
                .user(testUser)
                .name("Test API Service")
                .endpointUrl("https://api.test.com")
                .budget(500.0)
                .costPerUnit(0.01)
                .usageCount(10000.0)
                .build();
        testApiService = apiServiceRepository.save(testApiService);

        UserPrincipal userPrincipal = UserPrincipal.create(testUser);
        Authentication authentication = new org.springframework.security.authentication.TestingAuthenticationToken(
                userPrincipal, null, "ROLE_USER");
        authentication.setAuthenticated(true);
        jwtToken = jwtTokenUtil.generateJwtToken(authentication);
    }

    @Test
    void testCreateUsageLog_Success() throws Exception {
        UsageLogRequest request = new UsageLogRequest();
        request.setApiServiceId(testApiService.getId());
        request.setDate(LocalDate.now().minusDays(1));
        request.setRequestsMade(1000);
        request.setSuccessCount(950);
        request.setErrorCount(50);
        request.setPeakHour(14);

        mockMvc.perform(post("/api/usage")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestsMade").value(1000))
                .andExpect(jsonPath("$.successCount").value(950))
                .andExpect(jsonPath("$.errorCount").value(50))
                .andExpect(jsonPath("$.peakHour").value(14))
                .andExpect(jsonPath("$.successRate").value(95.0))
                .andExpect(jsonPath("$.errorRate").value(5.0))
                .andExpect(jsonPath("$.peakHourFormatted").value("14:00"))
                .andExpect(jsonPath("$.costIncurred").value(10.0));
    }

    @Test
    void testCreateUsageLog_InvalidCounts() throws Exception {
        UsageLogRequest request = new UsageLogRequest();
        request.setApiServiceId(testApiService.getId());
        request.setDate(LocalDate.now().minusDays(1));
        request.setRequestsMade(100);
        request.setSuccessCount(80);
        request.setErrorCount(30); // 80 + 30 = 110 > 100 requests
        request.setPeakHour(10);

        mockMvc.perform(post("/api/usage")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot exceed total requests")));
    }

    @Test
    void testCreateUsageLog_DuplicateDate() throws Exception {
        // Create existing usage log
        UsageLog existingLog = UsageLog.builder()
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(1))
                .requestsMade(500)
                .successCount(450)
                .errorCount(50)
                .peakHour(12)
                .build();
        usageLogRepository.save(existingLog);

        UsageLogRequest request = new UsageLogRequest();
        request.setApiServiceId(testApiService.getId());
        request.setDate(LocalDate.now().minusDays(1)); // Same date
        request.setRequestsMade(600);
        request.setSuccessCount(550);
        request.setErrorCount(50);
        request.setPeakHour(15);

        mockMvc.perform(post("/api/usage")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void testGetUsageLogsByApiService() throws Exception {
        // Create multiple usage logs
        UsageLog log1 = UsageLog.builder()
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(2))
                .requestsMade(800)
                .successCount(760)
                .errorCount(40)
                .peakHour(10)
                .build();

        UsageLog log2 = UsageLog.builder()
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(1))
                .requestsMade(1200)
                .successCount(1140)
                .errorCount(60)
                .peakHour(15)
                .build();

        usageLogRepository.save(log1);
        usageLogRepository.save(log2);

        mockMvc.perform(get("/api/usage/" + testApiService.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].requestsMade").value(1200)) // Most recent first
                .andExpect(jsonPath("$[1].requestsMade").value(800))
                .andExpect(jsonPath("$[0].successRate").value(95.0))
                .andExpect(jsonPath("$[1].successRate").value(95.0));
    }

    @Test
    void testGetTopApi() throws Exception {
        // Create another API service
        ApiService anotherApiService = ApiService.builder()
                .user(testUser)
                .name("Another API Service")
                .endpointUrl("https://api.another.com")
                .budget(300.0)
                .costPerUnit(0.02)
                .usageCount(5000.0)
                .build();
        anotherApiService = apiServiceRepository.save(anotherApiService);

        // Create usage logs with different volumes
        UsageLog log1 = UsageLog.builder()
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(1))
                .requestsMade(2000) // Higher volume
                .successCount(1900)
                .errorCount(100)
                .peakHour(14)
                .build();

        UsageLog log2 = UsageLog.builder()
                .apiService(anotherApiService)
                .logDate(LocalDate.now().minusDays(1))
                .requestsMade(500) // Lower volume
                .successCount(450)
                .errorCount(50)
                .peakHour(10)
                .build();

        usageLogRepository.save(log1);
        usageLogRepository.save(log2);

        mockMvc.perform(get("/api/usage/top-api")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiServiceName").value("Test API Service"))
                .andExpect(jsonPath("$.totalRequests").value(2000))
                .andExpect(jsonPath("$.averageSuccessRate").value(95.0))
                .andExpect(jsonPath("$.averageErrorRate").value(5.0))
                .andExpect(jsonPath("$.mostCommonPeakHour").value(14))
                .andExpect(jsonPath("$.mostCommonPeakHourFormatted").value("14:00"))
                .andExpect(jsonPath("$.status").value("healthy")); // Changed from "warning" to "healthy"
    }

    @Test
    void testGetUsageSummary() throws Exception {
        // Create usage logs over multiple days
        for (int i = 1; i <= 7; i++) {
            UsageLog log = UsageLog.builder()
                    .apiService(testApiService)
                    .logDate(LocalDate.now().minusDays(i))
                    .requestsMade(1000 + (i * 100)) // Increasing trend
                    .successCount((int) ((1000 + (i * 100)) * 0.95))
                    .errorCount((int) ((1000 + (i * 100)) * 0.05))
                    .peakHour(12 + (i % 12)) // Varying peak hours
                    .build();
            usageLogRepository.save(log);
        }

        mockMvc.perform(get("/api/usage/summary")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(greaterThan(7000)))
                .andExpect(jsonPath("$.averageSuccessRate").value(closeTo(95.0, 1.0)))
                .andExpect(jsonPath("$.averageErrorRate").value(closeTo(5.0, 1.0)))
                .andExpect(jsonPath("$.mostCommonPeakHour").isNumber())
                .andExpect(jsonPath("$.topApis", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.totalDaysTracked").value(7))
                .andExpect(jsonPath("$.usageTrend").isString())
                .andExpect(jsonPath("$.qualityTrend").isString())
                .andExpect(jsonPath("$.insights", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.recommendations", hasSize(greaterThan(0))));
    }

    @Test
    void testGetUsageAnalytics() throws Exception {
        // Create comprehensive test data
        createTestUsageData();

        mockMvc.perform(get("/api/usage/analytics")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.summary.totalRequests").value(greaterThan(0)))
                .andExpect(jsonPath("$.recentLogs").isArray())
                .andExpect(jsonPath("$.highErrorLogs").isArray())
                .andExpect(jsonPath("$.alerts").isArray())
                .andExpect(jsonPath("$.optimizationSuggestions").isArray());
    }

    @Test
    void testGetUsageSummary_NoData() throws Exception {
        mockMvc.perform(get("/api/usage/summary")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(0))
                .andExpect(jsonPath("$.averageSuccessRate").value(0.0))
                .andExpect(jsonPath("$.averageErrorRate").value(0.0))
                .andExpect(jsonPath("$.topApis", hasSize(0)))
                .andExpect(jsonPath("$.usageTrend").value("insufficient_data"))
                .andExpect(jsonPath("$.insights[0]").value(containsString("No usage data available")));
    }

    @Test
    void testCreateUsageLog_InvalidPeakHour() throws Exception {
        UsageLogRequest request = new UsageLogRequest();
        request.setApiServiceId(testApiService.getId());
        request.setDate(LocalDate.now().minusDays(1));
        request.setRequestsMade(1000);
        request.setSuccessCount(950);
        request.setErrorCount(50);
        request.setPeakHour(25); // Invalid hour

        mockMvc.perform(post("/api/usage")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.peakHour").value(containsString("between 0 and 23")));
    }

    @Test
    void testCreateUsageLog_FutureDate() throws Exception {
        UsageLogRequest request = new UsageLogRequest();
        request.setApiServiceId(testApiService.getId());
        request.setDate(LocalDate.now().plusDays(1)); // Future date
        request.setRequestsMade(1000);
        request.setSuccessCount(950);
        request.setErrorCount(50);
        request.setPeakHour(14);

        mockMvc.perform(post("/api/usage")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.date").value(containsString("cannot be in the future")));
    }

    private void createTestUsageData() {
        // Create normal usage logs
        for (int i = 1; i <= 5; i++) {
            UsageLog normalLog = UsageLog.builder()
                    .apiService(testApiService)
                    .logDate(LocalDate.now().minusDays(i))
                    .requestsMade(1000)
                    .successCount(950)
                    .errorCount(50)
                    .peakHour(14)
                    .build();
            usageLogRepository.save(normalLog);
        }

        // Create high error log
        UsageLog highErrorLog = UsageLog.builder()
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(6))
                .requestsMade(1000)
                .successCount(800)
                .errorCount(200) // 20% error rate
                .peakHour(16)
                .build();
        usageLogRepository.save(highErrorLog);

        // Create high usage log
        UsageLog highUsageLog = UsageLog.builder()
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(7))
                .requestsMade(5000) // High usage
                .successCount(4750)
                .errorCount(250)
                .peakHour(18)
                .build();
        usageLogRepository.save(highUsageLog);
    }
}
