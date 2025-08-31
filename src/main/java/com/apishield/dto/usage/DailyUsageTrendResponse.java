package com.apishield.dto.usage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyUsageTrendResponse {
    private LocalDate date;
    private Long totalRequests;
    private Double successRate;
    private Double errorRate;
    private String dayOfWeek;
}
