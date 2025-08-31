package com.apishield.service;

import com.apishield.dto.usage.*;
import com.apishield.exception.BadRequestException;
import com.apishield.exception.ResourceNotFoundException;
import com.apishield.model.ApiService;
import com.apishield.model.UsageLog;
import com.apishield.model.User;
import com.apishield.repository.ApiServiceRepository;
import com.apishield.repository.UsageLogRepository;
import com.apishield.util.AnalyticsCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageLogService {

    private final UsageLogRepository usageLogRepository;
    private final ApiServiceRepository apiServiceRepository;
    private final AnalyticsCalculator analyticsCalculator;

    @Transactional
    public UsageLogResponse createUsageLog(User user, UsageLogRequest request) {
        log.info("Creating usage log for user: {} and API service: {}", user.getEmail(), request.getApiServiceId());

        // Validate API service ownership
        ApiService apiService = apiServiceRepository.findByIdAndUser(request.getApiServiceId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("API service not found"));

        // Validate request counts
        if (!request.isValidCounts()) {
            throw new BadRequestException("Success count + Error count cannot exceed total requests");
        }

        // Check if log for this date already exists
        Optional<UsageLog> existingLog = usageLogRepository
                .findByApiServiceAndLogDate(apiService, request.getDate());

        if (existingLog.isPresent()) {
            throw new BadRequestException("Usage log for this date already exists. Use update instead.");
        }

        UsageLog usageLog = UsageLog.builder()
                .apiService(apiService)
                .logDate(request.getDate())
                .requestsMade(request.getRequestsMade())
                .successCount(request.getSuccessCount())
                .errorCount(request.getErrorCount())
                .peakHour(request.getPeakHour())
                .build();

        UsageLog savedLog = usageLogRepository.save(usageLog);
        log.info("Successfully created usage log with ID: {}", savedLog.getId());

        // Update API service usage count
        updateApiServiceUsageCount(apiService, request.getRequestsMade());

        return mapToUsageLogResponse(savedLog, Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public List<UsageLogResponse> getUsageLogsByApiService(User user, Long apiServiceId) {
        log.info("Fetching usage logs for API service: {} and user: {}", apiServiceId, user.getEmail());

        ApiService apiService = apiServiceRepository.findByIdAndUser(apiServiceId, user)
                .orElseThrow(() -> new ResourceNotFoundException("API service not found"));

        List<UsageLog> logs = usageLogRepository.findByApiServiceOrderByLogDateDesc(apiService);
        return logs.stream()
                .map(log -> mapToUsageLogResponse(log, logs))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TopApiResponse getTopApiByUser(User user) {
        log.info("Finding top API service by usage for user: {}", user.getEmail());

        Pageable topOne = PageRequest.of(0, 1);
        List<Object[]> results = usageLogRepository.findTopApiServicesByUser(user, topOne);

        if (results.isEmpty()) {
            return null;
        }

        Object[] result = results.get(0);
        Long apiServiceId = (Long) result[0];
        String apiServiceName = (String) result[1];
        Long totalRequests = (Long) result[2];

        // Get additional analytics for this API
        ApiService apiService = apiServiceRepository.findById(apiServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("API service not found"));

        List<UsageLog> logs = usageLogRepository.findByApiServiceOrderByLogDateDesc(apiService);
        double avgSuccessRate = analyticsCalculator.calculateOverallSuccessRate(logs);
        double avgErrorRate = analyticsCalculator.calculateOverallErrorRate(logs);
        Integer peakHour = analyticsCalculator.findMostCommonPeakHour(logs);
        double totalCost = logs.stream()
                .mapToDouble(log -> analyticsCalculator.calculateCostIncurred(log, apiService.getCostPerUnit()))
                .sum();

        String status = determineApiStatus(avgSuccessRate, avgErrorRate);

        return TopApiResponse.builder()
                .apiServiceId(apiServiceId)
                .apiServiceName(apiServiceName)
                .endpointUrl(apiService.getEndpointUrl())
                .totalRequests(totalRequests)
                .averageSuccessRate(avgSuccessRate)
                .averageErrorRate(avgErrorRate)
                .mostCommonPeakHour(peakHour)
                .mostCommonPeakHourFormatted(peakHour != null ? String.format("%02d:00", peakHour) : null)
                .totalCostIncurred(Math.round(totalCost * 100.0) / 100.0)
                .status(status)
                .build();
    }

    @Transactional(readOnly = true)
    public UsageSummaryResponse getUsageSummary(User user) {
        log.info("Generating usage summary for user: {}", user.getEmail());

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<UsageLog> recentLogs = usageLogRepository
                .findByUserAndDateRangeOrderByLogDateDesc(user, thirtyDaysAgo, LocalDate.now());

        if (recentLogs.isEmpty()) {
            return createEmptySummary();
        }

        // Calculate overall statistics
        long totalRequests = recentLogs.stream().mapToLong(UsageLog::getRequestsMade).sum();
        double avgSuccessRate = analyticsCalculator.calculateOverallSuccessRate(recentLogs);
        double avgErrorRate = analyticsCalculator.calculateOverallErrorRate(recentLogs);
        Integer mostCommonPeakHour = analyticsCalculator.findMostCommonPeakHour(recentLogs);

        // Get top APIs
        Pageable top5 = PageRequest.of(0, 5);
        List<Object[]> topApiResults = usageLogRepository.findTopApiServicesByUser(user, top5);
        List<TopApiResponse> topApis = topApiResults.stream()
                .map(this::mapToTopApiResponse)
                .collect(Collectors.toList());

        // Calculate trends
        String usageTrend = analyticsCalculator.analyzeUsageTrend(recentLogs);
        String qualityTrend = analyticsCalculator.analyzeQualityTrend(recentLogs);

        // Generate daily trends
        List<DailyUsageTrendResponse> dailyTrends = generateDailyTrends(recentLogs);

        // Generate insights and recommendations
        List<String> insights = analyticsCalculator.generateInsights(recentLogs);
        List<String> recommendations = analyticsCalculator.generateRecommendations(recentLogs);

        return UsageSummaryResponse.builder()
                .totalRequests(totalRequests)
                .averageSuccessRate(avgSuccessRate)
                .averageErrorRate(avgErrorRate)
                .mostCommonPeakHour(mostCommonPeakHour)
                .mostCommonPeakHourFormatted(mostCommonPeakHour != null ?
                        String.format("%02d:00", mostCommonPeakHour) : null)
                .topApis(topApis)
                .dateFrom(thirtyDaysAgo)
                .dateTo(LocalDate.now())
                .totalDaysTracked(recentLogs.stream()
                        .map(UsageLog::getLogDate)
                        .collect(Collectors.toSet()).size())
                .averageRequestsPerDay(Math.round((totalRequests / 30.0) * 100.0) / 100.0)
                .usageTrend(usageTrend)
                .qualityTrend(qualityTrend)
                .dailyTrends(dailyTrends)
                .insights(insights)
                .recommendations(recommendations)
                .build();
    }

    @Transactional(readOnly = true)
    public UsageAnalyticsResponse getUsageAnalytics(User user) {
        log.info("Generating comprehensive usage analytics for user: {}", user.getEmail());

        UsageSummaryResponse summary = getUsageSummary(user);

        // Get recent high activity logs
        Pageable recent10 = PageRequest.of(0, 10);
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<UsageLog> recentHighUsageLogs = usageLogRepository
                .findRecentHighUsageLogs(user, sevenDaysAgo, recent10);

        List<UsageLogResponse> recentLogs = recentHighUsageLogs.stream()
                .map(log -> mapToUsageLogResponse(log, Collections.emptyList()))
                .collect(Collectors.toList());

        // Get high error rate logs
        List<UsageLog> highErrorLogs = usageLogRepository.findHighErrorRateLogs(user, 0.05); // >5% error rate
        List<UsageLogResponse> highErrorLogResponses = highErrorLogs.stream()
                .limit(10)
                .map(log -> mapToUsageLogResponse(log, Collections.emptyList()))
                .collect(Collectors.toList());

        // Generate alerts
        List<String> alerts = generateAlerts(user, summary);

        // Generate optimization suggestions
        List<String> optimizationSuggestions = generateOptimizationSuggestions(summary);

        return UsageAnalyticsResponse.builder()
                .summary(summary)
                .recentLogs(recentLogs)
                .highErrorLogs(highErrorLogResponses)
                .alerts(alerts)
                .optimizationSuggestions(optimizationSuggestions)
                .build();
    }

    @Transactional
    private void updateApiServiceUsageCount(ApiService apiService, Integer newRequests) {
        // Update the usage count in the API service
        apiService.setUsageCount(apiService.getUsageCount() + newRequests);
        apiServiceRepository.save(apiService);

        log.info("Updated API service {} usage count to: {}",
                apiService.getId(), apiService.getUsageCount());
    }

    private UsageLogResponse mapToUsageLogResponse(UsageLog log, List<UsageLog> historicalLogs) {
        String status = analyticsCalculator.determineUsageStatus(log, historicalLogs);
        double costIncurred = analyticsCalculator.calculateCostIncurred(log, log.getApiService().getCostPerUnit());

        return UsageLogResponse.builder()
                .id(log.getId())
                .apiServiceId(log.getApiService().getId())
                .apiServiceName(log.getApiService().getName())
                .date(log.getLogDate())
                .requestsMade(log.getRequestsMade())
                .successCount(log.getSuccessCount())
                .errorCount(log.getErrorCount())
                .peakHour(log.getPeakHour())
                .peakHourFormatted(log.getPeakHourFormatted())
                .successRate(log.getSuccessRate())
                .errorRate(log.getErrorRate())
                .isHighErrorRate(log.isHighErrorRate())
                .createdAt(log.getCreatedAt())
                .status(status)
                .costIncurred(Math.round(costIncurred * 100.0) / 100.0)
                .build();
    }

    private TopApiResponse mapToTopApiResponse(Object[] result) {
        Long apiServiceId = (Long) result[0];
        String apiServiceName = (String) result[1];
        Long totalRequests = (Long) result[2];

        return TopApiResponse.builder()
                .apiServiceId(apiServiceId)
                .apiServiceName(apiServiceName)
                .totalRequests(totalRequests)
                .build();
    }

    private List<DailyUsageTrendResponse> generateDailyTrends(List<UsageLog> logs) {
        Map<LocalDate, List<UsageLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(UsageLog::getLogDate));

        return logsByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<UsageLog> dayLogs = entry.getValue();

                    long totalRequests = dayLogs.stream().mapToLong(UsageLog::getRequestsMade).sum();
                    double successRate = analyticsCalculator.calculateOverallSuccessRate(dayLogs);
                    double errorRate = analyticsCalculator.calculateOverallErrorRate(dayLogs);

                    return DailyUsageTrendResponse.builder()
                            .date(date)
                            .totalRequests(totalRequests)
                            .successRate(successRate)
                            .errorRate(errorRate)
                            .dayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                            .build();
                })
                .sorted(Comparator.comparing(DailyUsageTrendResponse::getDate).reversed())
                .collect(Collectors.toList());
    }

    private List<String> generateAlerts(User user, UsageSummaryResponse summary) {
        List<String> alerts = new ArrayList<>();

        if (summary.getAverageErrorRate() > 10) {
            alerts.add(String.format("High error rate alert: %.1f%% average error rate in the last 30 days",
                    summary.getAverageErrorRate()));
        }

        if ("increasing".equals(summary.getUsageTrend())) {
            alerts.add("Usage trending upward - consider monitoring costs and performance");
        }

        if ("declining".equals(summary.getQualityTrend())) {
            alerts.add("API quality declining - investigate recent changes");
        }

        return alerts;
    }

    private List<String> generateOptimizationSuggestions(UsageSummaryResponse summary) {
        List<String> suggestions = new ArrayList<>();

        if (summary.getAverageRequestsPerDay() > 1000) {
            suggestions.add("Consider implementing caching to reduce API calls");
        }

        if (summary.getMostCommonPeakHour() != null &&
                summary.getMostCommonPeakHour() >= 9 && summary.getMostCommonPeakHour() <= 17) {
            suggestions.add("Peak usage during business hours - consider load balancing");
        }

        if (summary.getAverageErrorRate() > 5) {
            suggestions.add("Implement retry logic and circuit breakers for better reliability");
        }

        return suggestions;
    }

    private String determineApiStatus(double successRate, double errorRate) {
        if (errorRate > 10) {
            return "critical";
        } else if (errorRate > 5 || successRate < 90) {
            return "warning";
        } else {
            return "healthy";
        }
    }

    private UsageSummaryResponse createEmptySummary() {
        return UsageSummaryResponse.builder()
                .totalRequests(0L)
                .averageSuccessRate(0.0)
                .averageErrorRate(0.0)
                .topApis(Collections.emptyList())
                .dateFrom(LocalDate.now().minusDays(30))
                .dateTo(LocalDate.now())
                .totalDaysTracked(0)
                .averageRequestsPerDay(0.0)
                .usageTrend("insufficient_data")
                .qualityTrend("insufficient_data")
                .dailyTrends(Collections.emptyList())
                .insights(Collections.singletonList("No usage data available yet. Start logging API usage to see insights."))
                .recommendations(Collections.singletonList("Begin tracking API usage to get personalized recommendations."))
                .build();
    }
}
