package com.apishield.service;

import com.apishield.dto.alert.*;
import com.apishield.exception.BadRequestException;
import com.apishield.exception.ResourceNotFoundException;
import com.apishield.model.*;
import com.apishield.repository.*;
import com.apishield.util.AlertProcessor;
import com.apishield.util.AnalyticsCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertThresholdRepository thresholdRepository;
    private final AlertRepository alertRepository;
    private final ApiServiceRepository apiServiceRepository;
    private final UsageLogRepository usageLogRepository;
    private final AlertProcessor alertProcessor;
    private final AnalyticsCalculator analyticsCalculator;

    @Transactional
    public AlertThresholdResponse createOrUpdateThreshold(User user, AlertThresholdRequest request) {
        log.info("Creating/updating alert threshold for user: {} and API service: {}",
                user.getEmail(), request.getApiServiceId());

        // Validate request
        if (!request.isValidThresholdOrder()) {
            throw new BadRequestException("Warning threshold must be less than critical threshold");
        }

        // Validate API service ownership
        ApiService apiService = apiServiceRepository.findByIdAndUser(request.getApiServiceId(), user)
                .orElseThrow(() -> new ResourceNotFoundException("API service not found"));

        // Find existing threshold or create new one
        AlertThreshold threshold = thresholdRepository.findByApiService(apiService)
                .orElse(AlertThreshold.builder().apiService(apiService).build());

        threshold.setWarningPercent(request.getWarningPercent());
        threshold.setCriticalPercent(request.getCriticalPercent());
        threshold.setSpikeThreshold(request.getSpikeThreshold());
        threshold.setErrorThreshold(request.getErrorThreshold());
        threshold.setIsEnabled(request.getIsEnabled());

        AlertThreshold savedThreshold = thresholdRepository.save(threshold);
        log.info("Successfully saved alert threshold with ID: {}", savedThreshold.getId());

        return mapToThresholdResponse(savedThreshold);
    }

    @Transactional(readOnly = true)
    public List<AlertThresholdResponse> getAllThresholds(User user) {
        log.info("Fetching all alert thresholds for user: {}", user.getEmail());

        List<AlertThreshold> thresholds = thresholdRepository.findByUser(user);
        return thresholds.stream()
                .map(this::mapToThresholdResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertActivityResponse getAlertActivity(User user) {
        log.info("Fetching alert activity for user: {}", user.getEmail());

        // Get recent alerts (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Alert> recentAlerts = alertRepository.findRecentAlerts(user, thirtyDaysAgo);

        List<AlertResponse> alertResponses = recentAlerts.stream()
                .limit(50) // Limit to 50 most recent
                .map(this::mapToAlertResponse)
                .collect(Collectors.toList());

        // Get unresolved counts by severity
        long totalUnresolved = alertRepository.countUnresolvedByUser(user);
        long criticalUnresolved = alertRepository.countUnresolvedBySeverity(user, Alert.Severity.CRITICAL);
        long highUnresolved = alertRepository.countUnresolvedBySeverity(user, Alert.Severity.HIGH);
        long mediumUnresolved = alertRepository.countUnresolvedBySeverity(user, Alert.Severity.MEDIUM);
        long lowUnresolved = alertRepository.countUnresolvedBySeverity(user, Alert.Severity.LOW);

        // Get alert statistics by type
        List<Object[]> typeStats = alertRepository.getAlertStatsByType(user, thirtyDaysAgo);
        Map<String, Long> alertsByType = typeStats.stream()
                .collect(Collectors.toMap(
                        stat -> ((Alert.AlertType) stat[0]).name(),
                        stat -> (Long) stat[1]
                ));

        // Get daily alert trend
        List<Object[]> dailyCounts = alertRepository.getDailyAlertCounts(user, thirtyDaysAgo);
        List<AlertActivityResponse.DailyAlertCount> dailyTrend = dailyCounts.stream()
                .map(count -> AlertActivityResponse.DailyAlertCount.builder()
                        .date(((java.sql.Date) count[0]).toLocalDate())
                        .count((Long) count[1])
                        .dayOfWeek(((java.sql.Date) count[0]).toLocalDate()
                                .getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                        .build())
                .collect(Collectors.toList());

        // Analyze trend
        String overallTrend = analyzeTrend(dailyTrend);

        // Generate insights and action items
        List<String> insights = generateInsights(recentAlerts, alertsByType);
        List<String> actionItems = generateActionItems(criticalUnresolved, highUnresolved, alertsByType);

        return AlertActivityResponse.builder()
                .recentAlerts(alertResponses)
                .totalUnresolvedAlerts(totalUnresolved)
                .criticalUnresolvedAlerts(criticalUnresolved)
                .highUnresolvedAlerts(highUnresolved)
                .mediumUnresolvedAlerts(mediumUnresolved)
                .lowUnresolvedAlerts(lowUnresolved)
                .alertsByType(alertsByType)
                .dailyAlertTrend(dailyTrend)
                .overallTrend(overallTrend)
                .insights(insights)
                .actionItems(actionItems)
                .build();
    }

    @Transactional
    public AlertResponse resolveAlert(User user, Long alertId) {
        log.info("Resolving alert ID: {} for user: {}", alertId, user.getEmail());

        Alert alert = alertRepository.findById(alertId)
                .filter(a -> a.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

        if (alert.getIsResolved()) {
            throw new BadRequestException("Alert is already resolved");
        }

        alert.resolve();
        Alert savedAlert = alertRepository.save(alert);
        log.info("Successfully resolved alert ID: {}", alertId);

        return mapToAlertResponse(savedAlert);
    }

    @Transactional
    public void deleteThreshold(User user, Long thresholdId) {
        log.info("Deleting alert threshold ID: {} for user: {}", thresholdId, user.getEmail());

        AlertThreshold threshold = thresholdRepository.findById(thresholdId)
                .filter(t -> t.getApiService().getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Alert threshold not found"));

        thresholdRepository.delete(threshold);
        log.info("Successfully deleted alert threshold ID: {}", thresholdId);
    }

    /**
     * Check all thresholds for a user and generate alerts if needed
     * Called by the scheduler
     */
    @Transactional
    public void checkAndGenerateAlerts(User user) {
        log.debug("Checking alerts for user: {}", user.getEmail());

        List<AlertThreshold> enabledThresholds = thresholdRepository.findEnabledByUser(user);

        for (AlertThreshold threshold : enabledThresholds) {
            checkApiServiceAlerts(threshold);
        }
    }

    /**
     * Check alerts for a specific API service
     */
    @Transactional
    public void checkApiServiceAlerts(AlertThreshold threshold) {
        ApiService apiService = threshold.getApiService();

        try {
            // Check budget utilization alerts
            double utilizationPercent = apiService.getUtilizationPercentage();
            Alert budgetAlert = alertProcessor.processBudgetAlert(apiService, threshold, utilizationPercent);
            if (budgetAlert != null) {
                log.info("Generated budget alert for API service: {}", apiService.getName());
            }

            // Check usage spike alerts
            checkUsageSpikes(threshold);

            // Check error rate alerts
            checkErrorRates(threshold);

            // Auto-resolve old alerts
            alertProcessor.autoResolveOldAlerts(apiService);

        } catch (Exception e) {
            log.error("Error checking alerts for API service: {}", apiService.getName(), e);
        }
    }

    private void checkUsageSpikes(AlertThreshold threshold) {
        ApiService apiService = threshold.getApiService();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate dayBeforeYesterday = LocalDate.now().minusDays(2);

        Optional<UsageLog> yesterdayLog = usageLogRepository.findYesterdayLog(apiService, yesterday);
        Optional<UsageLog> previousLog = usageLogRepository.findDayBeforeYesterdayLog(apiService, dayBeforeYesterday);

        if (yesterdayLog.isPresent() && previousLog.isPresent()) {
            int currentUsage = yesterdayLog.get().getRequestsMade();
            int previousUsage = previousLog.get().getRequestsMade();

            double spikePercent = analyticsCalculator.calculateSpike(currentUsage, previousUsage);

            Alert spikeAlert = alertProcessor.processUsageSpike(apiService, threshold,
                    spikePercent, currentUsage, previousUsage);
            if (spikeAlert != null) {
                log.info("Generated usage spike alert for API service: {}", apiService.getName());
            }
        }
    }

    private void checkErrorRates(AlertThreshold threshold) {
        ApiService apiService = threshold.getApiService();
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Optional<UsageLog> yesterdayLog = usageLogRepository.findYesterdayLog(apiService, yesterday);

        if (yesterdayLog.isPresent()) {
            UsageLog usageLog = yesterdayLog.get(); // Changed from 'log' to 'usageLog'
            double errorRate = usageLog.getErrorRate() / 100.0; // Convert percentage to decimal

            Alert errorAlert = alertProcessor.processErrorRateAlert(apiService, threshold,
                    errorRate, usageLog.getRequestsMade());
            if (errorAlert != null) {
                log.info("Generated error rate alert for API service: {}", apiService.getName());
            }
        }
    }


    private AlertThresholdResponse mapToThresholdResponse(AlertThreshold threshold) {
        ApiService apiService = threshold.getApiService();
        double currentUtilization = apiService.getUtilizationPercentage();

        String currentStatus = "safe";
        if (threshold.shouldTriggerCritical(currentUtilization)) {
            currentStatus = "critical";
        } else if (threshold.shouldTriggerWarning(currentUtilization)) {
            currentStatus = "warning";
        }

        // Check for recent alerts (last 24 hours)
        LocalDateTime dayAgo = LocalDateTime.now().minusHours(24);
        boolean hasRecentAlerts = !alertRepository.findRecentAlerts(apiService.getUser(), dayAgo)
                .stream()
                .filter(alert -> alert.getApiService() != null &&
                        alert.getApiService().getId().equals(apiService.getId()))
                .toList()
                .isEmpty();

        return AlertThresholdResponse.builder()
                .id(threshold.getId())
                .apiServiceId(apiService.getId())
                .apiServiceName(apiService.getName())
                .warningPercent(threshold.getWarningPercent())
                .criticalPercent(threshold.getCriticalPercent())
                .spikeThreshold(threshold.getSpikeThreshold())
                .errorThreshold(threshold.getErrorThreshold())
                .isEnabled(threshold.getIsEnabled())
                .createdAt(threshold.getCreatedAt())
                .updatedAt(threshold.getUpdatedAt())
                .currentUtilization(Math.round(currentUtilization * 100.0) / 100.0)
                .currentStatus(currentStatus)
                .hasRecentAlerts(hasRecentAlerts)
                .build();
    }

    private AlertResponse mapToAlertResponse(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .apiServiceName(alert.getApiService() != null ? alert.getApiService().getName() : "System")
                .alertType(alert.getAlertType())
                .message(alert.getMessage())
                .formattedMessage(alert.getFormattedMessage())
                .severity(alert.getSeverity())
                .thresholdValue(alert.getThresholdValue())
                .actualValue(alert.getActualValue())
                .isResolved(alert.getIsResolved())
                .resolvedAt(alert.getResolvedAt())
                .notificationSent(alert.getNotificationSent())
                .createdAt(alert.getCreatedAt())
                .minutesSinceCreated(alert.getMinutesSinceCreated())
                .alertTypeDescription(alertProcessor.getAlertTypeDescription(alert.getAlertType()))
                .severityColor(alertProcessor.getSeverityColor(alert.getSeverity()))
                .actionRequired(alertProcessor.getActionRequired(alert))
                .build();
    }

    private String analyzeTrend(List<AlertActivityResponse.DailyAlertCount> dailyTrend) {
        if (dailyTrend.size() < 7) {
            return "insufficient_data";
        }

        // Get first week and last week averages
        List<AlertActivityResponse.DailyAlertCount> sorted = dailyTrend.stream()
                .sorted(Comparator.comparing(AlertActivityResponse.DailyAlertCount::getDate))
                .collect(Collectors.toList());

        double firstWeekAvg = sorted.subList(0, Math.min(7, sorted.size()))
                .stream().mapToLong(AlertActivityResponse.DailyAlertCount::getCount).average().orElse(0);

        double lastWeekAvg = sorted.subList(Math.max(0, sorted.size() - 7), sorted.size())
                .stream().mapToLong(AlertActivityResponse.DailyAlertCount::getCount).average().orElse(0);

        double change = ((lastWeekAvg - firstWeekAvg) / Math.max(1, firstWeekAvg)) * 100;

        if (change > 20) {
            return "increasing";
        } else if (change < -20) {
            return "decreasing";
        } else {
            return "stable";
        }
    }

    private List<String> generateInsights(List<Alert> recentAlerts, Map<String, Long> alertsByType) {
        List<String> insights = new ArrayList<>();

        if (recentAlerts.isEmpty()) {
            insights.add("No recent alerts - your API usage is well within configured thresholds");
            return insights;
        }

        // Most common alert type
        String mostCommonType = alertsByType.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (mostCommonType != null) {
            long count = alertsByType.get(mostCommonType);
            insights.add(String.format("Most frequent alert type: %s (%d occurrences)",
                    mostCommonType.replace("_", " ").toLowerCase(), count));
        }

        // Critical alerts
        long criticalCount = recentAlerts.stream()
                .filter(alert -> alert.getSeverity() == Alert.Severity.CRITICAL)
                .count();

        if (criticalCount > 0) {
            insights.add(String.format("%d critical alerts require immediate attention", criticalCount));
        }

        // Unresolved alerts
        long unresolvedCount = recentAlerts.stream()
                .filter(alert -> !Boolean.TRUE.equals(alert.getIsResolved()))
                .count();

        if (unresolvedCount > 5) {
            insights.add(String.format("%d unresolved alerts - consider reviewing and resolving old alerts",
                    unresolvedCount));
        }

        return insights;
    }

    private List<String> generateActionItems(long criticalUnresolved, long highUnresolved,
                                             Map<String, Long> alertsByType) {
        List<String> actionItems = new ArrayList<>();

        if (criticalUnresolved > 0) {
            actionItems.add(String.format("Resolve %d critical alerts immediately", criticalUnresolved));
        }

        if (highUnresolved > 0) {
            actionItems.add(String.format("Review and address %d high-priority alerts", highUnresolved));
        }

        // Specific recommendations based on alert types
        if (alertsByType.getOrDefault("BUDGET_CRITICAL", 0L) > 0) {
            actionItems.add("Consider increasing budgets or optimizing API usage for cost-critical services");
        }

        if (alertsByType.getOrDefault("HIGH_ERROR_RATE", 0L) > 0) {
            actionItems.add("Investigate API reliability issues and implement better error handling");
        }

        if (alertsByType.getOrDefault("USAGE_SPIKE", 0L) > 2) {
            actionItems.add("Review usage patterns and consider implementing rate limiting");
        }

        if (actionItems.isEmpty()) {
            actionItems.add("Continue monitoring - no immediate actions required");
        }

        return actionItems;
    }
}
