package com.apishield.service;

import com.apishield.dto.alert.AlertThresholdRequest;
import com.apishield.dto.alert.AlertThresholdResponse;
import com.apishield.exception.BadRequestException;
import com.apishield.model.*;
import com.apishield.repository.*;
import com.apishield.util.AlertProcessor;
import com.apishield.util.AnalyticsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertThresholdRepository thresholdRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private ApiServiceRepository apiServiceRepository;
    @Mock
    private UsageLogRepository usageLogRepository;
    @Mock
    private AlertProcessor alertProcessor;
    @Mock
    private AnalyticsCalculator analyticsCalculator;

    @InjectMocks
    private AlertService alertService;

    private User testUser;
    private ApiService testApiService;
    private AlertThreshold testThreshold;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .role(User.Role.USER)
                .currency(User.Currency.USD)
                .build();

        testApiService = ApiService.builder()
                .id(1L)
                .user(testUser)
                .name("Test API")
                .endpointUrl("https://api.test.com")
                .budget(1000.0)
                .costPerUnit(0.01)
                .usageCount(75000.0) // 75% utilization
                .build();

        testThreshold = AlertThreshold.builder()
                .id(1L)
                .apiService(testApiService)
                .warningPercent(70.0)
                .criticalPercent(90.0)
                .spikeThreshold(50.0)
                .errorThreshold(0.1)
                .isEnabled(true)
                .build();
    }

    @Test
    void testCreateThreshold_Success() {
        AlertThresholdRequest request = new AlertThresholdRequest();
        request.setApiServiceId(1L);
        request.setWarningPercent(75.0);
        request.setCriticalPercent(90.0);
        request.setSpikeThreshold(50.0);
        request.setErrorThreshold(0.1);
        request.setIsEnabled(true);

        when(apiServiceRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testApiService));
        when(thresholdRepository.findByApiService(testApiService)).thenReturn(Optional.empty());
        when(thresholdRepository.save(any(AlertThreshold.class))).thenReturn(testThreshold);
        when(alertRepository.findRecentAlerts(any(), any())).thenReturn(Arrays.asList());

        AlertThresholdResponse response = alertService.createOrUpdateThreshold(testUser, request);

        assertNotNull(response);
        assertEquals(testApiService.getId(), response.getApiServiceId());
        verify(thresholdRepository).save(any(AlertThreshold.class));
    }

    @Test
    void testCreateThreshold_InvalidThresholdOrder() {
        AlertThresholdRequest request = new AlertThresholdRequest();
        request.setApiServiceId(1L);
        request.setWarningPercent(90.0); // Higher than critical
        request.setCriticalPercent(70.0);
        request.setSpikeThreshold(50.0);
        request.setErrorThreshold(0.1);

        assertThrows(BadRequestException.class, () ->
                alertService.createOrUpdateThreshold(testUser, request));
    }

    @Test
    void testGetAllThresholds() {
        when(thresholdRepository.findByUser(testUser)).thenReturn(Arrays.asList(testThreshold));
        when(alertRepository.findRecentAlerts(any(), any())).thenReturn(Arrays.asList());

        var thresholds = alertService.getAllThresholds(testUser);

        assertNotNull(thresholds);
        assertEquals(1, thresholds.size());
        assertEquals("Test API", thresholds.get(0).getApiServiceName());
    }

    @Test
    void testCheckAndGenerateAlerts() {
        when(thresholdRepository.findEnabledByUser(testUser)).thenReturn(Arrays.asList(testThreshold));
        when(alertProcessor.processBudgetAlert(any(), any(), anyDouble())).thenReturn(null);
        when(usageLogRepository.findYesterdayLog(any(), any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> alertService.checkAndGenerateAlerts(testUser));

        verify(thresholdRepository).findEnabledByUser(testUser);
        verify(alertProcessor).processBudgetAlert(any(), any(), anyDouble());
    }
}
