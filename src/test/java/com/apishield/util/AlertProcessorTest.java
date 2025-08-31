package com.apishield.util;

import com.apishield.model.*;
import com.apishield.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertProcessorTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertProcessor alertProcessor;

    private User testUser;
    private ApiService testApiService;
    private AlertThreshold testThreshold;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        testApiService = ApiService.builder()
                .id(1L)
                .user(testUser)
                .name("Test API")
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
    void testProcessBudgetAlert_Warning() {
        when(alertRepository.findDuplicateAlerts(any(), any(), any())).thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert alert = alertProcessor.processBudgetAlert(testApiService, testThreshold, 75.0);

        assertNotNull(alert);
        assertEquals(Alert.AlertType.BUDGET_WARNING, alert.getAlertType());
        assertEquals(Alert.Severity.HIGH, alert.getSeverity());
        assertTrue(alert.getMessage().contains("75.0%"));
        verify(alertRepository).save(any(Alert.class));
    }

    @Test
    void testProcessBudgetAlert_Critical() {
        when(alertRepository.findDuplicateAlerts(any(), any(), any())).thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert alert = alertProcessor.processBudgetAlert(testApiService, testThreshold, 95.0);

        assertNotNull(alert);
        assertEquals(Alert.AlertType.BUDGET_CRITICAL, alert.getAlertType());
        assertEquals(Alert.Severity.CRITICAL, alert.getSeverity());
        assertTrue(alert.getMessage().contains("95.0%"));
    }

    @Test
    void testProcessBudgetAlert_NoAlert() {
        Alert alert = alertProcessor.processBudgetAlert(testApiService, testThreshold, 50.0);

        assertNull(alert); // Below warning threshold
        verify(alertRepository, never()).save(any());
    }

    @Test
    void testProcessUsageSpike() {
        when(alertRepository.findDuplicateAlerts(any(), any(), any())).thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert alert = alertProcessor.processUsageSpike(testApiService, testThreshold, 75.0, 1750, 1000);

        assertNotNull(alert);
        assertEquals(Alert.AlertType.USAGE_SPIKE, alert.getAlertType());
        assertTrue(alert.getMessage().contains("75.0%"));
        assertTrue(alert.getMessage().contains("increase"));
    }

    @Test
    void testProcessErrorRateAlert() {
        when(alertRepository.findDuplicateAlerts(any(), any(), any())).thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Alert alert = alertProcessor.processErrorRateAlert(testApiService, testThreshold, 0.15, 1000);

        assertNotNull(alert);
        assertEquals(Alert.AlertType.HIGH_ERROR_RATE, alert.getAlertType());
        assertTrue(alert.getMessage().contains("15.0%"));
    }

    @Test
    void testGetAlertTypeDescription() {
        String description = alertProcessor.getAlertTypeDescription(Alert.AlertType.BUDGET_WARNING);
        assertEquals("Budget utilization approaching limit", description);
    }

    @Test
    void testGetSeverityColor() {
        assertEquals("#dc3545", alertProcessor.getSeverityColor(Alert.Severity.CRITICAL));
        assertEquals("#fd7e14", alertProcessor.getSeverityColor(Alert.Severity.HIGH));
        assertEquals("#ffc107", alertProcessor.getSeverityColor(Alert.Severity.MEDIUM));
        assertEquals("#28a745", alertProcessor.getSeverityColor(Alert.Severity.LOW));
    }

    @Test
    void testDuplicateAlertPrevention() {
        Alert existingAlert = Alert.builder()
                .apiService(testApiService)
                .alertType(Alert.AlertType.BUDGET_WARNING)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(alertRepository.findDuplicateAlerts(any(), any(), any())).thenReturn(Collections.singletonList(existingAlert));

        Alert alert = alertProcessor.processBudgetAlert(testApiService, testThreshold, 75.0);

        assertNull(alert); // Should prevent duplicate
        verify(alertRepository, never()).save(any());
    }
}
