package com.apishield.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertActivityResponse {
    private List<AlertResponse> recentAlerts;
    private long totalUnresolvedAlerts;
    private long criticalUnresolvedAlerts;
    private long highUnresolvedAlerts;
    private long mediumUnresolvedAlerts;
    private long lowUnresolvedAlerts;

    // Statistics
    private Map<String, Long> alertsByType;
    private List<DailyAlertCount> dailyAlertTrend;
    private String overallTrend; // "increasing", "decreasing", "stable"

    // Summary insights
    private List<String> insights;
    private List<String> actionItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAlertCount {
        private LocalDate date;
        private Long count;
        private String dayOfWeek;
    }
}
