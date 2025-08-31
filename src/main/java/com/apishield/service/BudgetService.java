package com.apishield.service;

import com.apishield.dto.budget.BudgetRequest;
import com.apishield.dto.budget.BudgetResponse;
import com.apishield.dto.budget.BudgetUpdateRequest;
import com.apishield.exception.ResourceNotFoundException;
import com.apishield.model.Budget;
import com.apishield.model.User;
import com.apishield.repository.ApiServiceRepository;
import com.apishield.repository.BudgetRepository;
import com.apishield.util.BudgetCalculator;
import com.apishield.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ApiServiceRepository apiServiceRepository;
    private final BudgetCalculator budgetCalculator;
    private final DateUtil dateUtil;

    @Transactional
    public BudgetResponse createOrUpdateBudget(User user, BudgetRequest request) {
        log.info("Creating/updating budget for user: {} with amount: {}", user.getEmail(), request.getMonthlyBudget());

        Budget budget = budgetRepository.findByUser(user)
                .orElse(Budget.builder()
                        .user(user)
                        .spentAmount(0.0)
                        .build());

        budget.setMonthlyBudget(request.getMonthlyBudget());

        // Recalculate spent amount from API services
        Double totalSpent = calculateTotalSpentFromApiServices(user);
        budget.setSpentAmount(totalSpent);

        Budget savedBudget = budgetRepository.save(budget);
        log.info("Successfully created/updated budget with ID: {}", savedBudget.getId());

        return mapToBudgetResponse(savedBudget, user);
    }

    @Transactional
    public BudgetResponse updateBudget(User user, BudgetUpdateRequest request) {
        log.info("Updating budget for user: {}", user.getEmail());

        Budget budget = budgetRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found for user"));

        if (request.getMonthlyBudget() != null) {
            budget.setMonthlyBudget(request.getMonthlyBudget());
        }

        // Recalculate spent amount
        Double totalSpent = calculateTotalSpentFromApiServices(user);
        budget.setSpentAmount(totalSpent);

        Budget updatedBudget = budgetRepository.save(budget);
        log.info("Successfully updated budget with ID: {}", updatedBudget.getId());

        return mapToBudgetResponse(updatedBudget, user);
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(User user) {
        log.info("Fetching budget for user: {}", user.getEmail());

        Budget budget = budgetRepository.findByUser(user)
                .orElse(createDefaultBudget(user));

        // Always recalculate spent amount for accuracy
        Double totalSpent = calculateTotalSpentFromApiServices(user);
        if (!budget.getSpentAmount().equals(totalSpent)) {
            budget.setSpentAmount(totalSpent);
            if (budget.getId() != null) {
                budgetRepository.save(budget);
            }
        }

        return mapToBudgetResponse(budget, user);
    }

    @Transactional
    public void refreshBudgetSpentAmount(User user) {
        log.info("Refreshing spent amount for user: {}", user.getEmail());

        Budget budget = budgetRepository.findByUser(user).orElse(null);
        if (budget != null) {
            Double totalSpent = calculateTotalSpentFromApiServices(user);
            budget.setSpentAmount(totalSpent);
            budgetRepository.save(budget);
            log.info("Updated spent amount to: {} for user: {}", totalSpent, user.getEmail());
        }
    }

    @Transactional(readOnly = true)
    public boolean hasBudget(User user) {
        return budgetRepository.existsByUser(user);
    }

    @Transactional(readOnly = true)
    public boolean isOverBudget(User user) {
        return budgetRepository.findByUser(user)
                .map(Budget::isOverBudget)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public double getBudgetUtilizationPercentage(User user) {
        return budgetRepository.findByUser(user)
                .map(Budget::getUtilizationPercentage)
                .orElse(0.0);
    }

    private Double calculateTotalSpentFromApiServices(User user) {
        return apiServiceRepository.getTotalSpentByUser(user).orElse(0.0);
    }

    private Budget createDefaultBudget(User user) {
        return Budget.builder()
                .user(user)
                .monthlyBudget(0.0)
                .spentAmount(0.0)
                .build();
    }

    private BudgetResponse mapToBudgetResponse(Budget budget, User user) {
        int daysLeftInMonth = dateUtil.getDaysLeftInMonth();
        double dailySpendingRate = dateUtil.calculateDailySpendingRate(budget.getSpentAmount());
        double projectedSpending = dateUtil.projectMonthlySpending(budget.getSpentAmount());

        // Get API services statistics
        long totalApiServices = apiServiceRepository.countByUser(user);
        double averageSpendPerService = totalApiServices > 0 ?
                budget.getSpentAmount() / totalApiServices : 0.0;

        String currencySymbol = budgetCalculator.getCurrencySymbol(user.getCurrencyPreference());

        return BudgetResponse.builder()
                .id(budget.getId())
                .monthlyBudget(budget.getMonthlyBudget())
                .spentAmount(budget.getSpentAmount())
                .remainingBudget(budget.getRemainingBudget())
                .utilizationPercentage(Math.round(budget.getUtilizationPercentage() * 100.0) / 100.0)
                .status(budget.getStatus())
                .isOverBudget(budget.isOverBudget())
                .daysLeftInMonth(daysLeftInMonth)
                .dailySpendingRate(Math.round(dailySpendingRate * 100.0) / 100.0)
                .projectedMonthlySpending(Math.round(projectedSpending * 100.0) / 100.0)
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .currencySymbol(currencySymbol)
                .monthlyBudgetInUserCurrency(budget.getMonthlyBudget())
                .spentAmountInUserCurrency(budget.getSpentAmount())
                .remainingBudgetInUserCurrency(budget.getRemainingBudget())
                .totalApiServices((int) totalApiServices)
                .averageSpendPerService(Math.round(averageSpendPerService * 100.0) / 100.0)
                .build();
    }
}
