package com.apishield.util;

import com.apishield.model.ApiService;
import com.apishield.model.User;
import com.apishield.model.UsageLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsCalculatorTest {

    private AnalyticsCalculator analyticsCalculator;
    private List<UsageLog> testLogs;

    @BeforeEach
    void setUp() {
        analyticsCalculator = new AnalyticsCalculator();

        User testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        ApiService testApiService = ApiService.builder()
                .id(1L)
                .user(testUser)
                .name("Test API")
                .costPerUnit(0.01)
                .build();

        UsageLog log1 = UsageLog.builder()
                .id(1L)
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(3))
                .requestsMade(1000)
                .successCount(950)
                .errorCount(50)
                .peakHour(14)
                .build();

        UsageLog log2 = UsageLog.builder()
                .id(2L)
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(2))
                .requestsMade(1200)
                .successCount(1140)
                .errorCount(60)
                .peakHour(15)
                .build();

        UsageLog log3 = UsageLog.builder()
                .id(3L)
                .apiService(testApiService)
                .logDate(LocalDate.now().minusDays(1))
                .requestsMade(1500)
                .successCount(1425)
                .errorCount(75)
                .peakHour(14)
                .build();

        testLogs = Arrays.asList(log1, log2, log3);
    }

    @Test
    void testCalculateSpike() {
        double spike = analyticsCalculator.calculateSpike(1500, 1000);
        assertEquals(50.0, spike, 0.01);
    }

    @Test
    void testCalculateSpike_ZeroPrevious() {
        double spike = analyticsCalculator.calculateSpike(1000, 0);
        assertEquals(100.0, spike);
    }

    @Test
    void testIsSignificantSpike() {
        assertTrue(analyticsCalculator.isSignificantSpike(30.0, 25.0));
        assertFalse(analyticsCalculator.isSignificantSpike(20.0, 25.0));
    }

    @Test
    void testCalculateOverallSuccessRate() {
        double successRate = analyticsCalculator.calculateOverallSuccessRate(testLogs);
        // Total requests: 3700, Total successes: 3515
        assertEquals(95.0, successRate, 0.01);
    }

    @Test
    void testCalculateOverallErrorRate() {
        double errorRate = analyticsCalculator.calculateOverallErrorRate(testLogs);
        // Total requests: 3700, Total errors: 185
        assertEquals(5.0, errorRate, 0.01);
    }

    @Test
    void testFindMostCommonPeakHour() {
        Integer mostCommonHour = analyticsCalculator.findMostCommonPeakHour(testLogs);
        assertEquals(14, mostCommonHour); // Appears twice
    }

    @Test
    void testAnalyzeUsageTrend_Increasing() {
        String trend = analyticsCalculator.analyzeUsageTrend(testLogs);
        assertEquals("increasing", trend);
    }

    @Test
    void testAnalyzeQualityTrend_Stable() {
        String trend = analyticsCalculator.analyzeQualityTrend(testLogs);
        assertEquals("stable", trend); // Success rate stays around 95%
    }

    @Test
    void testGenerateInsights() {
        List<String> insights = analyticsCalculator.generateInsights(testLogs);
        assertFalse(insights.isEmpty());
        assertTrue(insights.stream().anyMatch(insight -> insight.contains("afternoon")));
    }

    @Test
    void testGenerateRecommendations() {
        List<String> recommendations = analyticsCalculator.generateRecommendations(testLogs);
        assertFalse(recommendations.isEmpty());
    }

    @Test
    void testCalculateCostIncurred() {
        UsageLog log = testLogs.get(0);
        double cost = analyticsCalculator.calculateCostIncurred(log, 0.01);
        assertEquals(10.0, cost, 0.01); // 1000 requests * 0.01
    }

    @Test
    void testDetermineUsageStatus_Normal() {
        UsageLog normalLog = testLogs.get(0);
        String status = analyticsCalculator.determineUsageStatus(normalLog, testLogs);
        assertEquals("normal", status);
    }

    @Test
    void testEmptyLogsHandling() {
        List<UsageLog> emptyLogs = Collections.emptyList();

        assertEquals(0.0, analyticsCalculator.calculateOverallSuccessRate(emptyLogs));
        assertEquals(0.0, analyticsCalculator.calculateOverallErrorRate(emptyLogs));
        assertNull(analyticsCalculator.findMostCommonPeakHour(emptyLogs));
        assertEquals("insufficient_data", analyticsCalculator.analyzeUsageTrend(emptyLogs));
        assertEquals("insufficient_data", analyticsCalculator.analyzeQualityTrend(emptyLogs));

        List<String> insights = analyticsCalculator.generateInsights(emptyLogs);
        assertTrue(insights.get(0).contains("No usage data available"));
    }
}
