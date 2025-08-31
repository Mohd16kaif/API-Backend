package com.apishield.service;

import com.apishield.dto.usage.UsageLogRequest;
import com.apishield.dto.usage.UsageLogResponse;
import com.apishield.dto.usage.UsageSummaryResponse;
import com.apishield.exception.BadRequestException;
import com.apishield.exception.ResourceNotFoundException;
import com.apishield.model.ApiService;
import com.apishield.model.User;
import com.apishield.model.UsageLog;
import com.apishield.repository.ApiServiceRepository;
import com.apishield.repository.UsageLogRepository;
import com.apishield.util.AnalyticsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageLogServiceTest {

    @Mock
    private UsageLogRepository usageLogRepository;

    @Mock
    private ApiServiceRepository apiServiceRepository;

    @Mock
    private AnalyticsCalculator analyticsCalculator;

    @InjectMocks
    private UsageLogService usageLogService;

    private User testUser;
    private ApiService testApiService;
    private UsageLog testUsageLog;

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
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(1000.0)
                .build();

        testUsageLog = UsageLog.builder()
                .id(1L)
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(1))
                .requestsMade(1000)
                .successCount(950)
                .errorCount(50)
                .peakHour(14)
                .build();
    }

    @Test
    void testCreateUsageLog_Success() {
        UsageLogRequest request = new UsageLogRequest();
        request.setApiServiceId(1L);
        request.setDate(LocalDate.now().minusDays(1));
        request.setRequestsMade(1000);
        request.setSuccessCount(950);
        request.setErrorCount(50);
        request.setPeakHour(14);

        when(apiServiceRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testApiService));
        when(usageLogRepository.findByApiServiceAndLogDate(testApiService, request.getDate()))
                .thenReturn(Optional.empty());
        when(usageLogRepository.save(any(UsageLog.class))).thenReturn(testUsageLog);
        when(apiServiceRepository.save(any(ApiService.class))).thenReturn(testApiService);
        when(analyticsCalculator.determineUsageStatus(any(), any())).thenReturn("normal");
        when(analyticsCalculator.calculateCostIncurred(any(), anyDouble())).thenReturn(10.0);

        UsageLogResponse response = usageLogService.createUsageLog(testUser, request);

        assertNotNull(response);
        assertEquals(1000, response.getRequestsMade());
        assertEquals(95.0, response.getSuccessRate());
        assertEquals(5.0, response.getErrorRate());
        verify(usageLogRepository).save(any(UsageLog.class));
        verify(apiServiceRepository).save(testApiService);
    }

    @Test
    void testCreateUsageLog_InvalidCounts() {
        UsageLogRequest request = new UsageLogRequest();
        request.setApiServiceId(1L);
        request.setDate(LocalDate.now().minusDays(1));
        request.setRequestsMade(100);
        request.setSuccessCount(80);
        request.setErrorCount(30); // 80 + 30 = 110 > 100
        request.setPeakHour(14);

        when(apiServiceRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testApiService));

        assertThrows(BadRequestException.class, () ->
                usageLogService.createUsageLog(testUser, request));
    }

    @Test
    void testCreateUsageLog_DuplicateDate() {
        UsageLogRequest request = new UsageLogRequest();
        request.setApiServiceId(1L);
        request.setDate(LocalDate.now().minusDays(1));
        request.setRequestsMade(1000);
        request.setSuccessCount(950);
        request.setErrorCount(50);
        request.setPeakHour(14);

        when(apiServiceRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testApiService));
        when(usageLogRepository.findByApiServiceAndLogDate(testApiService, request.getDate()))
                .thenReturn(Optional.of(testUsageLog));

        assertThrows(BadRequestException.class, () ->
                usageLogService.createUsageLog(testUser, request));
    }

    @Test
    void testCreateUsageLog_ApiServiceNotFound() {
        UsageLogRequest request = new UsageLogRequest();
        request.setApiServiceId(999L);
        request.setDate(LocalDate.now().minusDays(1));
        request.setRequestsMade(1000);
        request.setSuccessCount(950);
        request.setErrorCount(50);
        request.setPeakHour(14);

        when(apiServiceRepository.findByIdAndUser(999L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                usageLogService.createUsageLog(testUser, request));
    }

    @Test
    void testGetUsageLogsByApiService() {
        when(apiServiceRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testApiService));
        when(usageLogRepository.findByApiServiceOrderByLogDateDesc(testApiService))
                .thenReturn(Arrays.asList(testUsageLog));
        when(analyticsCalculator.determineUsageStatus(any(), any())).thenReturn("normal");
        when(analyticsCalculator.calculateCostIncurred(any(), anyDouble())).thenReturn(10.0);

        List<UsageLogResponse> responses = usageLogService.getUsageLogsByApiService(testUser, 1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1000, responses.get(0).getRequestsMade());
    }

    @Test
    void testGetUsageSummary_WithData() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<UsageLog> logs = Arrays.asList(testUsageLog);

        when(usageLogRepository.findByUserAndDateRangeOrderByLogDateDesc(testUser, thirtyDaysAgo, LocalDate.now()))
                .thenReturn(logs);
        when(analyticsCalculator.calculateOverallSuccessRate(logs)).thenReturn(95.0);
        when(analyticsCalculator.calculateOverallErrorRate(logs)).thenReturn(5.0);
        when(analyticsCalculator.findMostCommonPeakHour(logs)).thenReturn(14);
        when(analyticsCalculator.analyzeUsageTrend(logs)).thenReturn("stable");
        when(analyticsCalculator.analyzeQualityTrend(logs)).thenReturn("stable");
        when(analyticsCalculator.generateInsights(logs)).thenReturn(Arrays.asList("Good performance"));
        when(analyticsCalculator.generateRecommendations(logs)).thenReturn(Arrays.asList("Keep it up"));
        when(usageLogRepository.findTopApiServicesByUser(eq(testUser), any())).thenReturn(Collections.emptyList());

        UsageSummaryResponse summary = usageLogService.getUsageSummary(testUser);

        assertNotNull(summary);
        assertEquals(1000L, summary.getTotalRequests());
        assertEquals(95.0, summary.getAverageSuccessRate());
        assertEquals(5.0, summary.getAverageErrorRate());
        assertEquals(14, summary.getMostCommonPeakHour());
        assertEquals("stable", summary.getUsageTrend());
        assertEquals("stable", summary.getQualityTrend());
    }

    @Test
    void testGetUsageSummary_NoData() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        when(usageLogRepository.findByUserAndDateRangeOrderByLogDateDesc(testUser, thirtyDaysAgo, LocalDate.now()))
                .thenReturn(Collections.emptyList());

        UsageSummaryResponse summary = usageLogService.getUsageSummary(testUser);

        assertNotNull(summary);
        assertEquals(0L, summary.getTotalRequests());
        assertEquals(0.0, summary.getAverageSuccessRate());
        assertEquals(0.0, summary.getAverageErrorRate());
        assertEquals("insufficient_data", summary.getUsageTrend());
        assertTrue(summary.getInsights().get(0).contains("No usage data available"));
    }
}
