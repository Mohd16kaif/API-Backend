package com.apishield.controller;

import com.apishield.dto.budget.BudgetRequest;
import com.apishield.dto.budget.BudgetResponse;
import com.apishield.dto.budget.BudgetUpdateRequest;
import com.apishield.model.User;
import com.apishield.service.BudgetService;
import com.apishield.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Budget", description = "Budget management APIs")
public class BudgetController {

    private final BudgetService budgetService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create or update monthly budget")
    public ResponseEntity<BudgetResponse> createOrUpdateBudget(
            Authentication authentication,
            @Valid @RequestBody BudgetRequest request) {
        User user = userService.getCurrentUserEntity(authentication);
        BudgetResponse response = budgetService.createOrUpdateBudget(user, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get current budget information")
    public ResponseEntity<BudgetResponse> getBudget(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        BudgetResponse response = budgetService.getBudget(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Update monthly budget")
    public ResponseEntity<BudgetResponse> updateBudget(
            Authentication authentication,
            @Valid @RequestBody BudgetUpdateRequest request) {
        User user = userService.getCurrentUserEntity(authentication);
        BudgetResponse response = budgetService.updateBudget(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh spent amount from API services")
    public ResponseEntity<BudgetResponse> refreshBudget(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        budgetService.refreshBudgetSpentAmount(user);
        BudgetResponse response = budgetService.getBudget(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @Operation(summary = "Get budget status summary")
    public ResponseEntity<Map<String, Object>> getBudgetStatus(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        BudgetResponse budget = budgetService.getBudget(user);

        Map<String, Object> status = Map.of(
                "hasBudget", budgetService.hasBudget(user),
                "isOverBudget", budget.isOverBudget(),
                "utilizationPercentage", budget.getUtilizationPercentage(),
                "status", budget.getStatus(),
                "daysLeftInMonth", budget.getDaysLeftInMonth(),
                "projectedMonthlySpending", budget.getProjectedMonthlySpending(),
                "willExceedBudget", budget.getProjectedMonthlySpending() > budget.getMonthlyBudget()
        );

        return ResponseEntity.ok(status);
    }

    @GetMapping("/insights")
    @Operation(summary = "Get budget insights and recommendations")
    public ResponseEntity<Map<String, Object>> getBudgetInsights(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        BudgetResponse budget = budgetService.getBudget(user);

        String recommendation = generateRecommendation(budget);
        String spendingTrend = analyzeTrend(budget);

        Map<String, Object> insights = Map.of(
                "recommendation", recommendation,
                "spendingTrend", spendingTrend,
                "dailyBudgetRemaining", budget.getRemainingBudget() / budget.getDaysLeftInMonth(),
                "spendingVelocity", budget.getDailySpendingRate(),
                "budgetHealthScore", calculateHealthScore(budget)
        );

        return ResponseEntity.ok(insights);
    }

    private String generateRecommendation(BudgetResponse budget) {
        double utilization = budget.getUtilizationPercentage();
        double projectedSpending = budget.getProjectedMonthlySpending();
        double monthlyBudget = budget.getMonthlyBudget();

        if (projectedSpending > monthlyBudget * 1.1) {
            return "Consider reducing API usage or increasing your budget. Current spending trend will exceed budget by " +
                    Math.round((projectedSpending - monthlyBudget) * 100) / 100.0;
        } else if (utilization > 90) {
            return "You're close to your budget limit. Monitor usage carefully for the rest of the month.";
        } else if (utilization > 75) {
            return "You're on track but approaching your budget limit. Consider optimizing API usage.";
        } else if (utilization < 25 && budget.getDaysLeftInMonth() < 10) {
            return "You're well under budget. Consider increasing usage of beneficial APIs or reducing budget for next month.";
        } else {
            return "Your spending is healthy and on track with your budget.";
        }
    }

    private String analyzeTrend(BudgetResponse budget) {
        double dailyRate = budget.getDailySpendingRate();
        double projectedSpending = budget.getProjectedMonthlySpending();
        double monthlyBudget = budget.getMonthlyBudget();

        if (projectedSpending > monthlyBudget * 1.2) {
            return "accelerating";
        } else if (projectedSpending > monthlyBudget * 1.05) {
            return "increasing";
        } else if (projectedSpending < monthlyBudget * 0.8) {
            return "conservative";
        } else {
            return "steady";
        }
    }

    private int calculateHealthScore(BudgetResponse budget) {
        double utilization = budget.getUtilizationPercentage();
        double projectedSpending = budget.getProjectedMonthlySpending();
        double monthlyBudget = budget.getMonthlyBudget();

        int score = 100;

        // Deduct points for high utilization
        if (utilization > 90) {
            score -= 30;
        } else if (utilization > 75) {
            score -= 15;
        }

        // Deduct points for projected overspending
        if (projectedSpending > monthlyBudget) {
            score -= 25;
        }

        // Deduct points for being over budget
        if (budget.isOverBudget()) {
            score -= 40;
        }

        return Math.max(0, score);
    }
}
