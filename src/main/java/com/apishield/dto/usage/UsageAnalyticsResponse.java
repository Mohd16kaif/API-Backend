package com.apishield.dto.usage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageAnalyticsResponse {
    private UsageSummaryResponse summary;
    private List<UsageLogResponse> recentLogs;
    private List<UsageLogResponse> highErrorLogs;
    private List<String> alerts;
    private List<String> optimizationSuggestions;
}
