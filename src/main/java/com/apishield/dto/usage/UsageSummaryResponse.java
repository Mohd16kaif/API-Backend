package com.apishield.dto.usage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageSummaryResponse {
    private Long totalRequests;
    private Double averageSuccessRate;
    private Double averageErrorRate;
    private Integer mostCommonPeakHour;
    private String mostCommonPeakHourFormatted;
    private List<TopApiResponse> topApis;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Integer totalDaysTracked;
    private Double averageRequestsPerDay;

    // Trends
    private String usageTrend; // "increasing", "decreasing", "stable"
    private String qualityTrend; // "improving", "declining", "stable"
    private List<DailyUsageTrendResponse> dailyTrends;

    // Insights
    private List<String> insights;
    private List<String> recommendations;
}
