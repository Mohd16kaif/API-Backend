package com.apishield.dto.service;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ApiServiceRequest {

    @NotBlank(message = "API service name is required")
    @Size(min = 2, max = 128, message = "Name must be between 2 and 128 characters")
    private String name;

    @NotBlank(message = "Endpoint URL is required")
    @Size(max = 512, message = "URL must not exceed 512 characters")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String endpointUrl;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Budget must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Budget must not exceed 999,999.99")
    private Double budget;

    @NotNull(message = "Cost per unit is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost per unit must be greater than 0")
    @DecimalMax(value = "999.99", message = "Cost per unit must not exceed 999.99")
    private Double costPerUnit;

    @NotNull(message = "Usage count is required")
    @DecimalMin(value = "0.0", message = "Usage count must be 0 or greater")
    private Double usageCount = 0.0;
}
