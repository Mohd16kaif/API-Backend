package com.apishield.dto.usage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageLogResponse {
    private Long id;
    private Long apiServiceId;
    private String apiServiceName;
    private LocalDate date;
    private Integer requestsMade;
    private Integer successCount;
    private Integer errorCount;
    private Integer peakHour;
    private String peakHourFormatted;
    private Double successRate;
    private Double errorRate;
    private boolean isHighErrorRate;
    private LocalDateTime createdAt;

    // Analytics fields
    private String status; // "normal", "high_usage", "high_error"
    private Integer rankByRequests; // Rank compared to other days
    private Double costIncurred; // Based on API service cost per unit
}
