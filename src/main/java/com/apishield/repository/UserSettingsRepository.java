package com.apishield.repository;

import com.apishield.model.User;
import com.apishield.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUser(User user);

    @Query("SELECT us FROM UserSettings us WHERE us.user.id = :userId")
    Optional<UserSettings> findByUserId(@Param("userId") Long userId);

    @Query("SELECT us FROM UserSettings us WHERE us.currencyPreference = :currency")
    List<UserSettings> findByCurrencyPreference(@Param("currency") User.Currency currency);

    @Query("SELECT us FROM UserSettings us WHERE us.emailNotifications = true AND us.budgetAlerts = true")
    List<UserSettings> findUsersWithBudgetAlertsEnabled();

    @Query("SELECT us FROM UserSettings us WHERE us.emailNotifications = true AND us.weeklyReports = true")
    List<UserSettings> findUsersWithWeeklyReportsEnabled();

    @Query("SELECT us.currencyPreference, COUNT(us) FROM UserSettings us GROUP BY us.currencyPreference")
    List<Object[]> getCurrencyPreferenceStats();

    @Query("SELECT us.theme, COUNT(us) FROM UserSettings us GROUP BY us.theme")
    List<Object[]> getThemePreferenceStats();

    @Query("SELECT us.timezone, COUNT(us) FROM UserSettings us GROUP BY us.timezone ORDER BY COUNT(us) DESC")
    List<Object[]> getTimezoneStats();

    boolean existsByUser(User user);

    long countByEmailNotifications(Boolean emailNotifications);
}
