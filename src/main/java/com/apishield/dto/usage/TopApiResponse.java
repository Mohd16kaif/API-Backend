package com.apishield.dto.usage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopApiResponse {
    private Long apiServiceId;
    private String apiServiceName;
    private String endpointUrl;
    private Long totalRequests;
    private Double averageSuccessRate;
    private Double averageErrorRate;
    private Integer mostCommonPeakHour;
    private String mostCommonPeakHourFormatted;
    private Double totalCostIncurred;
    private String status; // "healthy", "warning", "critical"
}
