package com.apishield.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    private DateUtil dateUtil;

    @BeforeEach
    void setUp() {
        dateUtil = new DateUtil();
    }

    @Test
    void testGetDaysLeftInMonth() {
        int daysLeft = dateUtil.getDaysLeftInMonth();

        LocalDate today = LocalDate.now();
        int expectedDaysLeft = today.lengthOfMonth() - today.getDayOfMonth() + 1;

        assertEquals(expectedDaysLeft, daysLeft);
    }

    @Test
    void testGetDaysElapsedInMonth() {
        int daysElapsed = dateUtil.getDaysElapsedInMonth();

        assertEquals(LocalDate.now().getDayOfMonth(), daysElapsed);
    }

    @Test
    void testGetDaysInCurrentMonth() {
        int daysInMonth = dateUtil.getDaysInCurrentMonth();

        assertEquals(YearMonth.now().lengthOfMonth(), daysInMonth);
    }

    @Test
    void testCalculateDailySpendingRate() {
        double spentAmount = 150.0;

        double dailyRate = dateUtil.calculateDailySpendingRate(spentAmount);

        int daysElapsed = LocalDate.now().getDayOfMonth();
        double expectedRate = daysElapsed > 0 ? spentAmount / daysElapsed : 0.0;

        assertEquals(expectedRate, dailyRate, 0.01);
    }

    @Test
    void testProjectMonthlySpending() {
        double spentAmount = 100.0;

        double projectedSpending = dateUtil.projectMonthlySpending(spentAmount);

        int daysElapsed = LocalDate.now().getDayOfMonth();
        int totalDays = YearMonth.now().lengthOfMonth();
        double expectedProjection = daysElapsed > 0 ? (spentAmount / daysElapsed) * totalDays : 0.0;

        assertEquals(expectedProjection, projectedSpending, 0.01);
    }

    @Test
    void testIsFirstWeekOfMonth() {
        boolean isFirstWeek = dateUtil.isFirstWeekOfMonth();

        assertEquals(LocalDate.now().getDayOfMonth() <= 7, isFirstWeek);
    }

    @Test
    void testIsLastWeekOfMonth() {
        boolean isLastWeek = dateUtil.isLastWeekOfMonth();

        LocalDate today = LocalDate.now();
        int daysInMonth = today.lengthOfMonth();

        assertEquals(today.getDayOfMonth() > (daysInMonth - 7), isLastWeek);
    }
}
