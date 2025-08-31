package com.apishield.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertThresholdResponse {
    private Long id;
    private Long apiServiceId;
    private String apiServiceName;
    private Double warningPercent;
    private Double criticalPercent;
    private Double spikeThreshold;
    private Double errorThreshold;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Current status
    private Double currentUtilization;
    private String currentStatus; // "safe", "warning", "critical"
    private Boolean hasRecentAlerts;
}
