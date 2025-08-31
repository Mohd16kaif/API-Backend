package com.apishield.dto.alert;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AlertThresholdRequest {

    @NotNull(message = "API service ID is required")
    private Long apiServiceId;

    @NotNull(message = "Warning threshold is required")
    @DecimalMin(value = "0.0", message = "Warning threshold must be non-negative")
    @DecimalMax(value = "100.0", message = "Warning threshold cannot exceed 100%")
    private Double warningPercent;

    @NotNull(message = "Critical threshold is required")
    @DecimalMin(value = "0.0", message = "Critical threshold must be non-negative")
    @DecimalMax(value = "100.0", message = "Critical threshold cannot exceed 100%")
    private Double criticalPercent;

    @NotNull(message = "Spike threshold is required")
    @DecimalMin(value = "0.0", message = "Spike threshold must be non-negative")
    @DecimalMax(value = "1000.0", message = "Spike threshold cannot exceed 1000%")
    private Double spikeThreshold;

    @NotNull(message = "Error threshold is required")
    @DecimalMin(value = "0.0", message = "Error threshold must be non-negative")
    @DecimalMax(value = "1.0", message = "Error threshold cannot exceed 1.0 (100%)")
    private Double errorThreshold;

    @NotNull(message = "Enabled flag is required")
    private Boolean isEnabled = true;

    // Custom validation
    public boolean isValidThresholdOrder() {
        return warningPercent < criticalPercent;
    }
}
