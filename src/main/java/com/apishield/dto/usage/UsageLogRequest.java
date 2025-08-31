package com.apishield.dto.usage;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UsageLogRequest {

    @NotNull(message = "API service ID is required")
    private Long apiServiceId;

    @NotNull(message = "Log date is required")
    @PastOrPresent(message = "Log date cannot be in the future")
    private LocalDate date;

    @NotNull(message = "Requests made count is required")
    @Min(value = 0, message = "Requests made must be non-negative")
    @Max(value = 1000000, message = "Requests made cannot exceed 1,000,000")
    private Integer requestsMade;

    @NotNull(message = "Success count is required")
    @Min(value = 0, message = "Success count must be non-negative")
    private Integer successCount;

    @NotNull(message = "Error count is required")
    @Min(value = 0, message = "Error count must be non-negative")
    private Integer errorCount;

    @NotNull(message = "Peak hour is required")
    @Min(value = 0, message = "Peak hour must be between 0 and 23")
    @Max(value = 23, message = "Peak hour must be between 0 and 23")
    private Integer peakHour;

    // Custom validation method
    public boolean isValidCounts() {
        return successCount + errorCount <= requestsMade;
    }
}
