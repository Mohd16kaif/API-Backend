package com.apishield.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

@Component
public class DateUtil {

    /**
     * Get the number of days left in the current month
     */
    public int getDaysLeftInMonth() {
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        return (int) ChronoUnit.DAYS.between(today, lastDayOfMonth) + 1;
    }

    /**
     * Get the number of days elapsed in the current month
     */
    public int getDaysElapsedInMonth() {
        LocalDate today = LocalDate.now();
        return today.getDayOfMonth();
    }

    /**
     * Get the total number of days in the current month
     */
    public int getDaysInCurrentMonth() {
        return YearMonth.now().lengthOfMonth();
    }

    /**
     * Calculate daily spending rate based on current spending and days elapsed
     */
    public double calculateDailySpendingRate(double spentAmount) {
        int daysElapsed = getDaysElapsedInMonth();
        if (daysElapsed == 0) {
            return 0.0;
        }
        return spentAmount / daysElapsed;
    }

    /**
     * Project monthly spending based on current daily spending rate
     */
    public double projectMonthlySpending(double spentAmount) {
        double dailyRate = calculateDailySpendingRate(spentAmount);
        int totalDaysInMonth = getDaysInCurrentMonth();
        return dailyRate * totalDaysInMonth;
    }

    /**
     * Check if we're in the first week of the month
     */
    public boolean isFirstWeekOfMonth() {
        return LocalDate.now().getDayOfMonth() <= 7;
    }

    /**
     * Check if we're in the last week of the month
     */
    public boolean isLastWeekOfMonth() {
        LocalDate today = LocalDate.now();
        int daysInMonth = today.lengthOfMonth();
        return today.getDayOfMonth() > (daysInMonth - 7);
    }
}
