package com.apishield.controller;

import com.apishield.dto.alert.AlertThresholdRequest;
import com.apishield.model.*;
import com.apishield.repository.*;
import com.apishield.security.JwtTokenUtil;
import com.apishield.security.UserPrincipal;
import com.apishield.service.NotificationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.mockito.Mockito;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiServiceRepository apiServiceRepository;

    @Autowired
    private AlertThresholdRepository alertThresholdRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @TestBean
    private NotificationService notificationService;

    // Static factory method for @TestBean
    static NotificationService notificationService() {
        return Mockito.mock(NotificationService.class);
    }

    private String jwtToken;
    private User testUser;
    private ApiService testApiService;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        alertThresholdRepository.deleteAll();
        apiServiceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .name("Alert Test User")
                .email("alert@example.com")
                .username("alerttestuser")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .currency(User.Currency.USD)
                .build();
        testUser = userRepository.save(testUser);

        testApiService = ApiService.builder()
                .user(testUser)
                .name("Test API Service")
                .endpointUrl("https://api.test.com")
                .budget(1000.0)
                .costPerUnit(0.01)
                .usageCount(75000.0) // 75% utilization
                .build();
        testApiService = apiServiceRepository.save(testApiService);

        UserPrincipal userPrincipal = UserPrincipal.create(testUser);
        Authentication authentication = new org.springframework.security.authentication.TestingAuthenticationToken(
                userPrincipal, null, userPrincipal.getAuthorities());
        jwtToken = jwtTokenUtil.generateJwtToken(authentication);
    }

    @Test
    void testCreateAlertThreshold_Success() throws Exception {
        AlertThresholdRequest request = new AlertThresholdRequest();
        request.setApiServiceId(testApiService.getId());
        request.setWarningPercent(70.0);
        request.setCriticalPercent(90.0);
        request.setSpikeThreshold(50.0);
        request.setErrorThreshold(0.1);
        request.setIsEnabled(true);

        mockMvc.perform(post("/api/alerts/thresholds")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiServiceId").value(testApiService.getId()))
                .andExpect(jsonPath("$.warningPercent").value(70.0))
                .andExpect(jsonPath("$.criticalPercent").value(90.0))
                .andExpect(jsonPath("$.spikeThreshold").value(50.0))
                .andExpect(jsonPath("$.errorThreshold").value(0.1))
                .andExpect(jsonPath("$.isEnabled").value(true))
                .andExpect(jsonPath("$.currentUtilization").value(75.0))
                .andExpect(jsonPath("$.currentStatus").value("warning"));
    }

    @Test
    void testCreateAlertThreshold_InvalidThresholdOrder() throws Exception {
        AlertThresholdRequest request = new AlertThresholdRequest();
        request.setApiServiceId(testApiService.getId());
        request.setWarningPercent(90.0);
        request.setCriticalPercent(70.0);
        request.setSpikeThreshold(50.0);
        request.setErrorThreshold(0.1);
        request.setIsEnabled(true);

        mockMvc.perform(post("/api/alerts/thresholds")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Warning threshold must be less than critical")));
    }

    @Test
    void testGetAllThresholds() throws Exception {
        AlertThreshold threshold = AlertThreshold.builder()
                .apiService(testApiService)
                .warningPercent(75.0)
                .criticalPercent(90.0)
                .spikeThreshold(50.0)
                .errorThreshold(0.1)
                .isEnabled(true)
                .build();
        alertThresholdRepository.save(threshold);

        mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].apiServiceName").value("Test API Service"))
                .andExpect(jsonPath("$[0].warningPercent").value(75.0))
                .andExpect(jsonPath("$[0].currentStatus").value("warning"));
    }

    @Test
    void testGetAlertActivity() throws Exception {
        Alert criticalAlert = Alert.builder()
                .user(testUser)
                .apiService(testApiService)
                .alertType(Alert.AlertType.BUDGET_CRITICAL)
                .message("Budget exceeded")
                .severity(Alert.Severity.CRITICAL)
                .thresholdValue(90.0)
                .actualValue(95.0)
                .isResolved(false)
                .build();

        Alert resolvedAlert = Alert.builder()
                .user(testUser)
                .apiService(testApiService)
                .alertType(Alert.AlertType.BUDGET_WARNING)
                .message("Budget warning")
                .severity(Alert.Severity.HIGH)
                .thresholdValue(70.0)
                .actualValue(75.0)
                .isResolved(true)
                .build();

        alertRepository.save(criticalAlert);
        alertRepository.save(resolvedAlert);

        mockMvc.perform(get("/api/alerts/activity")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentAlerts", hasSize(2)))
                .andExpect(jsonPath("$.totalUnresolvedAlerts").value(1))
                .andExpect(jsonPath("$.criticalUnresolvedAlerts").value(1))
                .andExpect(jsonPath("$.highUnresolvedAlerts").value(0))
                .andExpect(jsonPath("$.alertsByType").exists())
                .andExpect(jsonPath("$.insights").isArray())
                .andExpect(jsonPath("$.actionItems").isArray());
    }

    @Test
    void testResolveAlert() throws Exception {
        Alert alert = Alert.builder()
                .user(testUser)
                .apiService(testApiService)
                .alertType(Alert.AlertType.BUDGET_WARNING)
                .message("Budget warning")
                .severity(Alert.Severity.HIGH)
                .isResolved(false)
                .build();
        alert = alertRepository.save(alert);

        mockMvc.perform(put("/api/alerts/" + alert.getId() + "/resolve")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isResolved").value(true))
                .andExpect(jsonPath("$.resolvedAt").exists());
    }

    @Test
    void testResolveAlert_AlreadyResolved() throws Exception {
        Alert alert = Alert.builder()
                .user(testUser)
                .apiService(testApiService)
                .alertType(Alert.AlertType.BUDGET_WARNING)
                .message("Budget warning")
                .severity(Alert.Severity.HIGH)
                .isResolved(true)
                .build();
        alert.resolve();
        alert = alertRepository.save(alert);

        mockMvc.perform(put("/api/alerts/" + alert.getId() + "/resolve")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already resolved")));
    }

    @Test
    void testDeleteThreshold() throws Exception {
        AlertThreshold threshold = AlertThreshold.builder()
                .apiService(testApiService)
                .warningPercent(75.0)
                .criticalPercent(90.0)
                .spikeThreshold(50.0)
                .errorThreshold(0.1)
                .isEnabled(true)
                .build();
        threshold = alertThresholdRepository.save(threshold);

        mockMvc.perform(delete("/api/alerts/thresholds/" + threshold.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/alerts")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testSendTestNotification() throws Exception {
        // Mock the notification service to not actually send emails
        doNothing().when(notificationService).sendTestNotification(anyString());

        mockMvc.perform(post("/api/alerts/test-notification")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("Test notification sent")));
    }

    @Test
    void testCheckAlertsNow() throws Exception {
        mockMvc.perform(post("/api/alerts/check-now")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(containsString("Alert check completed")));
    }

    @Test
    void testUpdateExistingThreshold() throws Exception {
        AlertThreshold existingThreshold = AlertThreshold.builder()
                .apiService(testApiService)
                .warningPercent(60.0)
                .criticalPercent(80.0)
                .spikeThreshold(30.0)
                .errorThreshold(0.05)
                .isEnabled(true)
                .build();
        alertThresholdRepository.save(existingThreshold);

        AlertThresholdRequest request = new AlertThresholdRequest();
        request.setApiServiceId(testApiService.getId());
        request.setWarningPercent(70.0);
        request.setCriticalPercent(90.0);
        request.setSpikeThreshold(50.0);
        request.setErrorThreshold(0.1);
        request.setIsEnabled(false);

        mockMvc.perform(post("/api/alerts/thresholds")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.warningPercent").value(70.0))
                .andExpect(jsonPath("$.criticalPercent").value(90.0))
                .andExpect(jsonPath("$.isEnabled").value(false));
    }

    @Test
    void testAlertThresholdValidation() throws Exception {
        AlertThresholdRequest request = new AlertThresholdRequest();
        request.setApiServiceId(testApiService.getId());
        request.setWarningPercent(-10.0);
        request.setCriticalPercent(150.0);
        request.setSpikeThreshold(2000.0);
        request.setErrorThreshold(2.0);

        mockMvc.perform(post("/api/alerts/thresholds")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").exists());
    }
}