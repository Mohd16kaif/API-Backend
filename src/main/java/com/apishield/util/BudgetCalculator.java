package com.apishield.util;

import com.apishield.model.ApiService;
import com.apishield.model.User;
import org.springframework.stereotype.Component;

@Component
public class BudgetCalculator {

    /**
     * Calculate utilization percentage for an API service
     */
    public double calculateUtilizationPercentage(ApiService apiService) {
        if (apiService.getBudget() == null || apiService.getBudget() == 0.0) {
            return 0.0;
        }
        double totalCost = apiService.getUsageCount() * apiService.getCostPerUnit();
        return Math.min(100.0, (totalCost / apiService.getBudget()) * 100.0);
    }

    /**
     * Calculate remaining budget for an API service
     */
    public double calculateRemainingBudget(ApiService apiService) {
        double totalCost = apiService.getUsageCount() * apiService.getCostPerUnit();
        return Math.max(0.0, apiService.getBudget() - totalCost);
    }

    /**
     * Calculate total spent for an API service
     */
    public double calculateTotalSpent(ApiService apiService) {
        return apiService.getUsageCount() * apiService.getCostPerUnit();
    }

    /**
     * Determine status based on utilization percentage
     */
//    public String determineStatus(double utilizationPercentage) {
//        if (utilizationPercentage >= 90.0) {
//            return "critical";
//        } else if (utilizationPercentage >= 75.0) {
//            return "warning";
//        } else {
//            return "healthy";
//        }
//    }
    public String determineStatus(double utilizationPercentage) {
        if (utilizationPercentage >= 100.0) {
            return "over_budget";   // ≥ 100% utilization (including 200%, 300%, etc.)
        } else if (utilizationPercentage >= 90.0) {
            return "critical";      // 90-99% utilization
        } else if (utilizationPercentage >= 75.0) {
            return "warning";       // 75-89% utilization
        } else {
            return "healthy";       // < 75% utilization
        }
    }

    /**
     * Convert amount based on user's currency preference
     */
    public double convertCurrency(double amount, User.Currency fromCurrency, User.Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return amount;
        }

        // Mock conversion rates (in real app, use external API)
        final double USD_TO_INR = 83.0;
        final double INR_TO_USD = 1.0 / USD_TO_INR;

        if (fromCurrency == User.Currency.USD && toCurrency == User.Currency.INR) {
            return amount * USD_TO_INR;
        } else if (fromCurrency == User.Currency.INR && toCurrency == User.Currency.USD) {
            return amount * INR_TO_USD;
        }

        return amount;
    }

    /**
     * Get currency symbol
     */
    public String getCurrencySymbol(User.Currency currency) {
        return currency == User.Currency.USD ? "$" : "₹";
    }
}
