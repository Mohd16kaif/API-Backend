package com.apishield.util;

import com.apishield.model.*;
import com.apishield.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertProcessor {

    private final AlertRepository alertRepository;

    /**
     * Check and create budget utilization alert
     */
    public Alert processBudgetAlert(ApiService apiService, AlertThreshold threshold, double utilizationPercent) {
        Alert.AlertType alertType;
        Alert.Severity severity;
        String message;

        if (threshold.shouldTriggerCritical(utilizationPercent)) {
            alertType = Alert.AlertType.BUDGET_CRITICAL;
            severity = Alert.Severity.CRITICAL;
            message = String.format("Critical budget alert: %.1f%% of budget used (threshold: %.1f%%)",
                    utilizationPercent, threshold.getCriticalPercent());
        } else if (threshold.shouldTriggerWarning(utilizationPercent)) {
            alertType = Alert.AlertType.BUDGET_WARNING;
            severity = Alert.Severity.HIGH;
            message = String.format("Budget warning: %.1f%% of budget used (threshold: %.1f%%)",
                    utilizationPercent, threshold.getWarningPercent());
        } else {
            return null; // No alert needed
        }

        // Check for duplicate alerts in the last 24 hours
        if (hasDuplicateAlert(apiService, alertType, LocalDateTime.now().minusHours(24))) {
            log.debug("Duplicate alert prevented for API service: {} and type: {}",
                    apiService.getName(), alertType);
            return null;
        }

        Alert alert = Alert.builder()
                .user(apiService.getUser())
                .apiService(apiService)
                .alertType(alertType)
                .message(message)
                .severity(severity)
                .thresholdValue(alertType == Alert.AlertType.BUDGET_CRITICAL ?
                        threshold.getCriticalPercent() : threshold.getWarningPercent())
                .actualValue(utilizationPercent)
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Check and create usage spike alert
     */
    public Alert processUsageSpike(ApiService apiService, AlertThreshold threshold,
                                   double spikePercent, int currentUsage, int previousUsage) {
        if (!threshold.shouldTriggerSpike(spikePercent)) {
            return null;
        }

        // Check for duplicate alerts in the last 6 hours (spikes are more frequent)
        if (hasDuplicateAlert(apiService, Alert.AlertType.USAGE_SPIKE, LocalDateTime.now().minusHours(6))) {
            log.debug("Duplicate spike alert prevented for API service: {}", apiService.getName());
            return null;
        }

        String direction = spikePercent > 0 ? "increase" : "decrease";
        String message = String.format("Usage spike detected: %.1f%% %s in API calls (%d → %d requests)",
                Math.abs(spikePercent), direction, previousUsage, currentUsage);

        Alert alert = Alert.builder()
                .user(apiService.getUser())
                .apiService(apiService)
                .alertType(Alert.AlertType.USAGE_SPIKE)
                .message(message)
                .severity(Math.abs(spikePercent) > 100 ? Alert.Severity.HIGH : Alert.Severity.MEDIUM)
                .thresholdValue(threshold.getSpikeThreshold())
                .actualValue(Math.abs(spikePercent))
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Check and create high error rate alert
     */
    public Alert processErrorRateAlert(ApiService apiService, AlertThreshold threshold,
                                       double errorRate, int totalRequests) {
        if (!threshold.shouldTriggerError(errorRate)) {
            return null;
        }

        // Check for duplicate alerts in the last 12 hours
        if (hasDuplicateAlert(apiService, Alert.AlertType.HIGH_ERROR_RATE, LocalDateTime.now().minusHours(12))) {
            log.debug("Duplicate error rate alert prevented for API service: {}", apiService.getName());
            return null;
        }

        String message = String.format("High error rate detected: %.1f%% error rate over %d requests (threshold: %.1f%%)",
                errorRate * 100, totalRequests, threshold.getErrorThreshold() * 100);

        Alert.Severity severity = errorRate > 0.2 ? Alert.Severity.CRITICAL :
                errorRate > 0.1 ? Alert.Severity.HIGH : Alert.Severity.MEDIUM;

        Alert alert = Alert.builder()
                .user(apiService.getUser())
                .apiService(apiService)
                .alertType(Alert.AlertType.HIGH_ERROR_RATE)
                .message(message)
                .severity(severity)
                .thresholdValue(threshold.getErrorThreshold())
                .actualValue(errorRate)
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Create cost anomaly alert
     */
    public Alert processCostAnomaly(ApiService apiService, double currentCost, double expectedCost) {
        double anomalyPercent = ((currentCost - expectedCost) / expectedCost) * 100;

        if (Math.abs(anomalyPercent) < 50) { // Only alert for significant anomalies
            return null;
        }

        // Check for duplicate alerts in the last 24 hours
        if (hasDuplicateAlert(apiService, Alert.AlertType.COST_ANOMALY, LocalDateTime.now().minusHours(24))) {
            return null;
        }

        String direction = anomalyPercent > 0 ? "higher" : "lower";
        String message = String.format("Cost anomaly detected: %.1f%% %s than expected ($%.2f vs $%.2f expected)",
                Math.abs(anomalyPercent), direction, currentCost, expectedCost);

        Alert alert = Alert.builder()
                .user(apiService.getUser())
                .apiService(apiService)
                .alertType(Alert.AlertType.COST_ANOMALY)
                .message(message)
                .severity(Math.abs(anomalyPercent) > 100 ? Alert.Severity.HIGH : Alert.Severity.MEDIUM)
                .actualValue(currentCost)
                .thresholdValue(expectedCost)
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Check if duplicate alert exists within time window
     */
    private boolean hasDuplicateAlert(ApiService apiService, Alert.AlertType alertType, LocalDateTime since) {
        List<Alert> duplicates = alertRepository.findDuplicateAlerts(apiService, alertType, since);
        return !duplicates.isEmpty();
    }

    /**
     * Get alert type description for UI
     */
    public String getAlertTypeDescription(Alert.AlertType alertType) {
        return switch (alertType) {
            case BUDGET_WARNING -> "Budget utilization approaching limit";
            case BUDGET_CRITICAL -> "Budget utilization exceeded critical threshold";
            case USAGE_SPIKE -> "Unusual change in API usage volume";
            case HIGH_ERROR_RATE -> "API error rate above acceptable threshold";
            case SERVICE_DOWN -> "API service appears to be unavailable";
            case COST_ANOMALY -> "Unexpected cost deviation detected";
        };
    }

    /**
     * Get severity color for UI styling
     */
    public String getSeverityColor(Alert.Severity severity) {
        return switch (severity) {
            case LOW -> "#28a745";      // Green
            case MEDIUM -> "#ffc107";   // Yellow
            case HIGH -> "#fd7e14";     // Orange
            case CRITICAL -> "#dc3545"; // Red
        };
    }

    /**
     * Generate action required message
     */
    public String getActionRequired(Alert alert) {
        return switch (alert.getAlertType()) {
            case BUDGET_WARNING -> "Monitor usage closely and consider optimizing API calls";
            case BUDGET_CRITICAL -> "Immediate action required: optimize usage or increase budget";
            case USAGE_SPIKE -> "Investigate cause of usage spike and validate legitimacy";
            case HIGH_ERROR_RATE -> "Check API service status and implement error handling";
            case SERVICE_DOWN -> "Contact API service provider and implement fallback";
            case COST_ANOMALY -> "Review recent usage patterns and validate charges";
        };
    }

    /**
     * Auto-resolve old alerts that are no longer relevant
     */
    public void autoResolveOldAlerts(ApiService apiService) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<Alert> oldAlerts = alertRepository.findByApiServiceOrderByCreatedAtDesc(apiService)
                .stream()
                .filter(alert -> !alert.getIsResolved() && alert.getCreatedAt().isBefore(cutoff))
                .toList();

        for (Alert alert : oldAlerts) {
            alert.resolve();
            alertRepository.save(alert);
            log.info("Auto-resolved old alert: {} for API service: {}",
                    alert.getAlertType(), apiService.getName());
        }
    }
}
