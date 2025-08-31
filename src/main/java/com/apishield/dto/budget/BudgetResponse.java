package com.apishield.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetResponse {
    private Long id;
    private Double monthlyBudget;
    private Double spentAmount;
    private Double remainingBudget;
    private Double utilizationPercentage;
    private String status; // "healthy", "warning", "critical", "over_budget"
    private boolean isOverBudget;
    private Integer daysLeftInMonth;
    private Double dailySpendingRate;
    private Double projectedMonthlySpending;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Currency-aware fields
    private String currencySymbol;
    private Double monthlyBudgetInUserCurrency;
    private Double spentAmountInUserCurrency;
    private Double remainingBudgetInUserCurrency;

    // Breakdown by API services
    private Integer totalApiServices;
    private Double averageSpendPerService;
}
