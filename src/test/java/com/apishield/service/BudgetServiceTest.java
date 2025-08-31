package com.apishield.service;

import com.apishield.dto.budget.BudgetRequest;
import com.apishield.dto.budget.BudgetResponse;
import com.apishield.model.Budget;
import com.apishield.model.User;
import com.apishield.repository.ApiServiceRepository;
import com.apishield.repository.BudgetRepository;
import com.apishield.util.BudgetCalculator;
import com.apishield.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private ApiServiceRepository apiServiceRepository;

    @Mock
    private BudgetCalculator budgetCalculator;

    @Mock
    private DateUtil dateUtil;

    @InjectMocks
    private BudgetService budgetService;

    private User testUser;
    private Budget testBudget;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .role(User.Role.USER)
                .currency(User.Currency.USD)  // Fixed: changed from currencyPreference to currency
                .build();

        testBudget = Budget.builder()
                .id(1L)
                .user(testUser)
                .monthlyBudget(1000.0)
                .spentAmount(250.0)
                .build();
    }

    @Test
    void testCreateBudget_NewBudget() {
        BudgetRequest request = new BudgetRequest();
        request.setMonthlyBudget(500.0);

        when(budgetRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(apiServiceRepository.getTotalSpentByUser(testUser)).thenReturn(Optional.of(100.0));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);
        when(dateUtil.getDaysLeftInMonth()).thenReturn(15);
        when(dateUtil.calculateDailySpendingRate(anyDouble())).thenReturn(5.0);
        when(dateUtil.projectMonthlySpending(anyDouble())).thenReturn(150.0);
        when(apiServiceRepository.countByUser(testUser)).thenReturn(2L);
        when(budgetCalculator.getCurrencySymbol(User.Currency.USD)).thenReturn("$");

        BudgetResponse response = budgetService.createOrUpdateBudget(testUser, request);

        assertNotNull(response);
        verify(budgetRepository).save(any(Budget.class));
        verify(apiServiceRepository).getTotalSpentByUser(testUser);
    }

    @Test
    void testGetBudget_ExistingBudget() {
        when(budgetRepository.findByUser(testUser)).thenReturn(Optional.of(testBudget));
        when(apiServiceRepository.getTotalSpentByUser(testUser)).thenReturn(Optional.of(250.0));
        when(dateUtil.getDaysLeftInMonth()).thenReturn(10);
        when(dateUtil.calculateDailySpendingRate(250.0)).thenReturn(12.5);
        when(dateUtil.projectMonthlySpending(250.0)).thenReturn(375.0);
        when(apiServiceRepository.countByUser(testUser)).thenReturn(3L);
        when(budgetCalculator.getCurrencySymbol(User.Currency.USD)).thenReturn("$");

        BudgetResponse response = budgetService.getBudget(testUser);

        assertNotNull(response);
        assertEquals(1000.0, response.getMonthlyBudget());
        assertEquals(250.0, response.getSpentAmount());
        assertEquals(10, response.getDaysLeftInMonth());
        assertEquals(12.5, response.getDailySpendingRate());
        assertEquals(375.0, response.getProjectedMonthlySpending());
    }

    @Test
    void testGetBudget_NoBudgetExists() {
        when(budgetRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(apiServiceRepository.getTotalSpentByUser(testUser)).thenReturn(Optional.of(0.0));
        when(dateUtil.getDaysLeftInMonth()).thenReturn(20);
        when(dateUtil.calculateDailySpendingRate(0.0)).thenReturn(0.0);
        when(dateUtil.projectMonthlySpending(0.0)).thenReturn(0.0);
        when(apiServiceRepository.countByUser(testUser)).thenReturn(0L);
        when(budgetCalculator.getCurrencySymbol(User.Currency.USD)).thenReturn("$");

        BudgetResponse response = budgetService.getBudget(testUser);

        assertNotNull(response);
        assertEquals(0.0, response.getMonthlyBudget());
        assertEquals(0.0, response.getSpentAmount());
    }

    @Test
    void testRefreshBudgetSpentAmount() {
        when(budgetRepository.findByUser(testUser)).thenReturn(Optional.of(testBudget));
        when(apiServiceRepository.getTotalSpentByUser(testUser)).thenReturn(Optional.of(300.0));
        when(budgetRepository.save(any(Budget.class))).thenReturn(testBudget);

        budgetService.refreshBudgetSpentAmount(testUser);

        verify(budgetRepository).save(any(Budget.class));
        verify(apiServiceRepository).getTotalSpentByUser(testUser);
    }

    @Test
    void testHasBudget() {
        when(budgetRepository.existsByUser(testUser)).thenReturn(true);

        boolean hasBudget = budgetService.hasBudget(testUser);

        assertTrue(hasBudget);
        verify(budgetRepository).existsByUser(testUser);
    }

    @Test
    void testIsOverBudget() {
        Budget overBudget = Budget.builder()
                .monthlyBudget(100.0)
                .spentAmount(150.0)
                .build();

        when(budgetRepository.findByUser(testUser)).thenReturn(Optional.of(overBudget));

        boolean isOver = budgetService.isOverBudget(testUser);

        assertTrue(isOver);
    }

    @Test
    void testGetBudgetUtilizationPercentage() {
        when(budgetRepository.findByUser(testUser)).thenReturn(Optional.of(testBudget));

        double utilization = budgetService.getBudgetUtilizationPercentage(testUser);

        assertEquals(25.0, utilization, 0.01); // 250/1000 * 100
    }
}