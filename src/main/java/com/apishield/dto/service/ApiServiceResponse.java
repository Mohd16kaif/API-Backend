package com.apishield.dto.service;

import com.apishield.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiServiceResponse {
    private Long id;
    private String name;
    private String endpointUrl;
    private Double budget;
    private Double costPerUnit;
    private Double usageCount;
    private Double utilizationPercentage;
    private Double remainingBudget;
    private Double totalSpent;
    private String status; // "healthy", "warning", "critical"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Currency-aware fields
    private String currencySymbol;
    private Double budgetInUserCurrency;
    private Double costPerUnitInUserCurrency;
    private Double totalSpentInUserCurrency;
    private Double remainingBudgetInUserCurrency;
}
