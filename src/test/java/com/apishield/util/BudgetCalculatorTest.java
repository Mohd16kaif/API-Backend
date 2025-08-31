package com.apishield.util;

import com.apishield.model.ApiService;
import com.apishield.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BudgetCalculatorTest {

    private BudgetCalculator budgetCalculator;
    private ApiService testApiService;

    @BeforeEach
    void setUp() {
        budgetCalculator = new BudgetCalculator();
        testApiService = ApiService.builder()
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(5000.0)
                .build();
    }

    @Test
    void testCalculateUtilizationPercentage() {
        double utilization = budgetCalculator.calculateUtilizationPercentage(testApiService);
        assertEquals(50.0, utilization, 0.01);
    }

    @Test
    void testCalculateUtilizationPercentage_ZeroBudget() {
        testApiService.setBudget(0.0);
        double utilization = budgetCalculator.calculateUtilizationPercentage(testApiService);
        assertEquals(0.0, utilization);
    }

    @Test
    void testCalculateUtilizationPercentage_OverBudget() {
        testApiService.setUsageCount(15000.0); // 150 total cost vs 100 budget
        double utilization = budgetCalculator.calculateUtilizationPercentage(testApiService);
        assertEquals(100.0, utilization); // Capped at 100%
    }

    @Test
    void testCalculateRemainingBudget() {
        double remaining = budgetCalculator.calculateRemainingBudget(testApiService);
        assertEquals(50.0, remaining, 0.01);
    }

    @Test
    void testCalculateRemainingBudget_OverBudget() {
        testApiService.setUsageCount(15000.0);
        double remaining = budgetCalculator.calculateRemainingBudget(testApiService);
        assertEquals(0.0, remaining);
    }

    @Test
    void testCalculateTotalSpent() {
        double totalSpent = budgetCalculator.calculateTotalSpent(testApiService);
        assertEquals(50.0, totalSpent, 0.01);
    }

    @Test
    void testDetermineStatus_Healthy() {
        String status = budgetCalculator.determineStatus(50.0);
        assertEquals("healthy", status);
    }

    @Test
    void testDetermineStatus_Warning() {
        String status = budgetCalculator.determineStatus(80.0);
        assertEquals("warning", status);
    }

    @Test
    void testDetermineStatus_Critical() {
        String status = budgetCalculator.determineStatus(95.0);
        assertEquals("critical", status);
    }

    @Test
    void testConvertCurrency_SameCurrency() {
        double converted = budgetCalculator.convertCurrency(100.0, User.Currency.USD, User.Currency.USD);
        assertEquals(100.0, converted);
    }

    @Test
    void testConvertCurrency_UsdToInr() {
        double converted = budgetCalculator.convertCurrency(100.0, User.Currency.USD, User.Currency.INR);
        assertEquals(8300.0, converted, 1.0); // Approximate due to mock rate
    }

    @Test
    void testGetCurrencySymbol() {
        assertEquals("$", budgetCalculator.getCurrencySymbol(User.Currency.USD));
        assertEquals("₹", budgetCalculator.getCurrencySymbol(User.Currency.INR));
    }
}
