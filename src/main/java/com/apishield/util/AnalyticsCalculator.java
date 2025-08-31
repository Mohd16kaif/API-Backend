package com.apishield.util;

import com.apishield.model.UsageLog;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AnalyticsCalculator {

    /**
     * Calculate usage spike percentage between two periods
     */
    public double calculateSpike(int currentUsage, int previousUsage) {
        if (previousUsage == 0) {
            return currentUsage > 0 ? 100.0 : 0.0;
        }
        return ((currentUsage - previousUsage) / (double) previousUsage) * 100.0;
    }

    /**
     * Determine if usage spike is significant
     */
    public boolean isSignificantSpike(double spikePercentage, double threshold) {
        return Math.abs(spikePercentage) >= threshold;
    }

    /**
     * Calculate overall success rate from multiple logs
     */
    public double calculateOverallSuccessRate(List<UsageLog> logs) {
        if (logs.isEmpty()) return 0.0;

        long totalRequests = logs.stream().mapToLong(UsageLog::getRequestsMade).sum();
        long totalSuccesses = logs.stream().mapToLong(UsageLog::getSuccessCount).sum();

        if (totalRequests == 0) return 0.0;
        return Math.round((totalSuccesses / (double) totalRequests) * 10000.0) / 100.0;
    }

    /**
     * Calculate overall error rate from multiple logs
     */
    public double calculateOverallErrorRate(List<UsageLog> logs) {
        if (logs.isEmpty()) return 0.0;

        long totalRequests = logs.stream().mapToLong(UsageLog::getRequestsMade).sum();
        long totalErrors = logs.stream().mapToLong(UsageLog::getErrorCount).sum();

        if (totalRequests == 0) return 0.0;
        return Math.round((totalErrors / (double) totalRequests) * 10000.0) / 100.0;
    }

    /**
     * Find most common peak hour from logs
     */
    public Integer findMostCommonPeakHour(List<UsageLog> logs) {
        if (logs.isEmpty()) return null;

        Map<Integer, Long> hourFrequency = logs.stream()
                .collect(Collectors.groupingBy(
                        UsageLog::getPeakHour,
                        Collectors.counting()
                ));

        return hourFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Analyze usage trend over time
     */
    public String analyzeUsageTrend(List<UsageLog> logs) {
        if (logs.size() < 3) return "insufficient_data";

        // Sort by date
        List<UsageLog> sortedLogs = logs.stream()
                .sorted(Comparator.comparing(UsageLog::getLogDate))
                .collect(Collectors.toList());

        // Calculate trend using linear regression or simple comparison
        int firstThird = sortedLogs.size() / 3;
        int lastThird = sortedLogs.size() - firstThird;

        double firstPeriodAvg = sortedLogs.subList(0, firstThird).stream()
                .mapToInt(UsageLog::getRequestsMade)
                .average()
                .orElse(0.0);

        double lastPeriodAvg = sortedLogs.subList(lastThird, sortedLogs.size()).stream()
                .mapToInt(UsageLog::getRequestsMade)
                .average()
                .orElse(0.0);

        double changePercentage = ((lastPeriodAvg - firstPeriodAvg) / firstPeriodAvg) * 100.0;

        if (changePercentage > 20) {
            return "increasing";
        } else if (changePercentage < -20) {
            return "decreasing";
        } else {
            return "stable";
        }
    }

    /**
     * Analyze quality trend (success/error rates over time)
     */
    public String analyzeQualityTrend(List<UsageLog> logs) {
        if (logs.size() < 3) return "insufficient_data";

        // Sort by date
        List<UsageLog> sortedLogs = logs.stream()
                .sorted(Comparator.comparing(UsageLog::getLogDate))
                .collect(Collectors.toList());

        int firstThird = sortedLogs.size() / 3;
        int lastThird = sortedLogs.size() - firstThird;

        double firstPeriodSuccessRate = calculateOverallSuccessRate(
                sortedLogs.subList(0, firstThird));
        double lastPeriodSuccessRate = calculateOverallSuccessRate(
                sortedLogs.subList(lastThird, sortedLogs.size()));

        double improvement = lastPeriodSuccessRate - firstPeriodSuccessRate;

        if (improvement > 5) {
            return "improving";
        } else if (improvement < -5) {
            return "declining";
        } else {
            return "stable";
        }
    }

    /**
     * Generate insights based on usage patterns
     */
    public List<String> generateInsights(List<UsageLog> logs) {
        List<String> insights = new ArrayList<>();

        if (logs.isEmpty()) {
            insights.add("No usage data available for analysis");
            return insights;
        }

        // Peak hour analysis
        Integer mostCommonHour = findMostCommonPeakHour(logs);
        if (mostCommonHour != null) {
            String timeOfDay = getTimeOfDayDescription(mostCommonHour);
            insights.add(String.format("Most API usage occurs during %s (%02d:00)", timeOfDay, mostCommonHour));
        }

        // Error rate analysis
        double avgErrorRate = calculateOverallErrorRate(logs);
        if (avgErrorRate > 10) {
            insights.add(String.format("High error rate detected (%.1f%%). Consider investigating API reliability", avgErrorRate));
        } else if (avgErrorRate < 1) {
            insights.add(String.format("Excellent API reliability with low error rate (%.1f%%)", avgErrorRate));
        }

        // Usage volume analysis
        long totalRequests = logs.stream().mapToLong(UsageLog::getRequestsMade).sum();
        double avgRequestsPerDay = totalRequests / (double) logs.size();
        if (avgRequestsPerDay > 10000) {
            insights.add("High volume API usage detected. Consider implementing caching strategies");
        }

        // Weekend vs weekday pattern
        analyzeWeekdayPatterns(logs, insights);

        return insights;
    }

    /**
     * Generate optimization recommendations
     */
    public List<String> generateRecommendations(List<UsageLog> logs) {
        List<String> recommendations = new ArrayList<>();

        if (logs.isEmpty()) {
            recommendations.add("Start logging API usage to get personalized recommendations");
            return recommendations;
        }

        double avgErrorRate = calculateOverallErrorRate(logs);
        if (avgErrorRate > 5) {
            recommendations.add("Implement retry logic and circuit breakers to handle API failures");
            recommendations.add("Consider adding request timeouts and error handling");
        }

        // Check for usage spikes
        boolean hasSpikes = logs.stream()
                .anyMatch(log -> log.getRequestsMade() > logs.stream()
                        .mapToInt(UsageLog::getRequestsMade)
                        .average().orElse(0) * 3);

        if (hasSpikes) {
            recommendations.add("Consider implementing rate limiting to prevent usage spikes");
            recommendations.add("Add monitoring alerts for unusual usage patterns");
        }

        // Peak hour optimization
        Integer peakHour = findMostCommonPeakHour(logs);
        if (peakHour != null && (peakHour >= 9 && peakHour <= 17)) {
            recommendations.add("Consider distributing API calls throughout the day to avoid peak business hours");
        }

        return recommendations;
    }

    private String getTimeOfDayDescription(int hour) {
        if (hour >= 6 && hour < 12) {
            return "morning";
        } else if (hour >= 12 && hour < 18) {
            return "afternoon";
        } else if (hour >= 18 && hour < 22) {
            return "evening";
        } else {
            return "night";
        }
    }

    private void analyzeWeekdayPatterns(List<UsageLog> logs, List<String> insights) {
        Map<String, Double> dayOfWeekUsage = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getLogDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        Collectors.averagingInt(UsageLog::getRequestsMade)
                ));

        if (dayOfWeekUsage.size() >= 7) {
            String busiestDay = dayOfWeekUsage.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Unknown");

            insights.add(String.format("Highest API usage typically occurs on %s", busiestDay));
        }
    }

    /**
     * Calculate cost incurred based on usage and cost per unit
     */
    public double calculateCostIncurred(UsageLog log, double costPerUnit) {
        return log.getRequestsMade() * costPerUnit;
    }

    /**
     * Determine status based on usage patterns
     */
    public String determineUsageStatus(UsageLog log, List<UsageLog> historicalLogs) {
        if (log.isHighErrorRate()) {
            return "high_error";
        }

        if (!historicalLogs.isEmpty()) {
            double avgRequests = historicalLogs.stream()
                    .mapToInt(UsageLog::getRequestsMade)
                    .average().orElse(0);

            if (log.getRequestsMade() > avgRequests * 2) {
                return "high_usage";
            }
        }

        return "normal";
    }
}
