package com.apishield.service;

import com.apishield.dto.settings.UserSettingsRequest;
import com.apishield.dto.settings.UserSettingsResponse;
import com.apishield.model.User;
import com.apishield.model.UserSettings;
import com.apishield.repository.UserSettingsRepository;
import com.apishield.util.CurrencyConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSettingsService {

    private final UserSettingsRepository settingsRepository;
    private final CurrencyConverter currencyConverter;

    @Transactional
    public UserSettingsResponse updateSettings(User user, UserSettingsRequest request) {
        log.info("Updating settings for user: {}", user.getEmail());

        UserSettings settings = settingsRepository.findByUser(user)
                .orElse(createDefaultSettings(user));

        // Update currency preference in both settings and user entity
        if (request.getCurrencyPreference() != null) {
            settings.setCurrencyPreference(request.getCurrencyPreference());
            user.setCurrencyPreference(request.getCurrencyPreference());
        }

        // Update other preferences
        updateFieldIfNotNull(settings::setTimezone, request.getTimezone());
        updateFieldIfNotNull(settings::setDateFormat, request.getDateFormat());
        updateFieldIfNotNull(settings::setTimeFormat, request.getTimeFormat());
        updateFieldIfNotNull(settings::setTheme, request.getTheme());
        updateFieldIfNotNull(settings::setLanguage, request.getLanguage());

        // Notification preferences
        updateFieldIfNotNull(settings::setEmailNotifications, request.getEmailNotifications());
        updateFieldIfNotNull(settings::setBudgetAlerts, request.getBudgetAlerts());
        updateFieldIfNotNull(settings::setUsageAlerts, request.getUsageAlerts());
        updateFieldIfNotNull(settings::setWeeklyReports, request.getWeeklyReports());
        updateFieldIfNotNull(settings::setMarketingEmails, request.getMarketingEmails());

        // Display preferences
        updateFieldIfNotNull(settings::setItemsPerPage, request.getItemsPerPage());
        updateFieldIfNotNull(settings::setDefaultChartPeriod, request.getDefaultChartPeriod());
        updateFieldIfNotNull(settings::setAutoRefreshDashboard, request.getAutoRefreshDashboard());
        updateFieldIfNotNull(settings::setDashboardRefreshInterval, request.getDashboardRefreshInterval());

        // Privacy preferences
        updateFieldIfNotNull(settings::setDataSharing, request.getDataSharing());
        updateFieldIfNotNull(settings::setAnalyticsCookies, request.getAnalyticsCookies());

        UserSettings savedSettings = settingsRepository.save(settings);
        log.info("Successfully updated settings for user: {}", user.getEmail());

        return mapToResponse(savedSettings);
    }

    @Transactional(readOnly = true)
    public UserSettingsResponse getSettings(User user) {
        log.info("Fetching settings for user: {}", user.getEmail());

        UserSettings settings = settingsRepository.findByUser(user)
                .orElse(createDefaultSettings(user));

        return mapToResponse(settings);
    }

    @Transactional
    public UserSettingsResponse updateCurrencyPreference(User user, User.Currency currency) {
        log.info("Updating currency preference for user: {} to: {}", user.getEmail(), currency);

        UserSettings settings = settingsRepository.findByUser(user)
                .orElse(createDefaultSettings(user));

        settings.setCurrencyPreference(currency);
        user.setCurrencyPreference(currency);

        UserSettings savedSettings = settingsRepository.save(settings);
        return mapToResponse(savedSettings);
    }

    @Transactional
    public UserSettingsResponse resetToDefaults(User user) {
        log.info("Resetting settings to defaults for user: {}", user.getEmail());

        UserSettings settings = settingsRepository.findByUser(user)
                .orElse(createDefaultSettings(user));

        // Reset to default values
        settings.setCurrencyPreference(User.Currency.USD);
        settings.setTimezone("UTC");
        settings.setDateFormat("MM/dd/yyyy");
        settings.setTimeFormat(UserSettings.TimeFormat.TWELVE_HOUR);
        settings.setTheme(UserSettings.Theme.LIGHT);
        settings.setLanguage("en");
        settings.setEmailNotifications(true);
        settings.setBudgetAlerts(true);
        settings.setUsageAlerts(true);
        settings.setWeeklyReports(true);
        settings.setMarketingEmails(false);
        settings.setItemsPerPage(20);
        settings.setDefaultChartPeriod(UserSettings.ChartPeriod.LAST_30_DAYS);
        settings.setAutoRefreshDashboard(true);
        settings.setDashboardRefreshInterval(300);
        settings.setDataSharing(false);
        settings.setAnalyticsCookies(true);

        UserSettings savedSettings = settingsRepository.save(settings);
        return mapToResponse(savedSettings);
    }

    @Transactional(readOnly = true)
    public List<UserSettings> getUsersWithNotificationsEnabled() {
        return settingsRepository.findUsersWithBudgetAlertsEnabled();
    }

    @Transactional(readOnly = true)
    public List<UserSettings> getUsersWithWeeklyReportsEnabled() {
        return settingsRepository.findUsersWithWeeklyReportsEnabled();
    }

    private UserSettings createDefaultSettings(User user) {
        return UserSettings.builder()
                .user(user)
                .currencyPreference(user.getCurrencyPreference())
                .timezone("UTC")
                .dateFormat("MM/dd/yyyy")
                .timeFormat(UserSettings.TimeFormat.TWELVE_HOUR)
                .theme(UserSettings.Theme.LIGHT)
                .language("en")
                .emailNotifications(true)
                .budgetAlerts(true)
                .usageAlerts(true)
                .weeklyReports(true)
                .marketingEmails(false)
                .itemsPerPage(20)
                .defaultChartPeriod(UserSettings.ChartPeriod.LAST_30_DAYS)
                .autoRefreshDashboard(true)
                .dashboardRefreshInterval(300)
                .dataSharing(false)
                .analyticsCookies(true)
                .build();
    }

    private UserSettingsResponse mapToResponse(UserSettings settings) {
        UserSettingsResponse.AvailableOptions options = UserSettingsResponse.AvailableOptions.builder()
                .timezones(getAvailableTimezones())
                .dateFormats(getAvailableDateFormats())
                .languages(getAvailableLanguages())
                .themes(Arrays.stream(UserSettings.Theme.values()).map(Enum::name).toList())
                .timeFormats(Arrays.stream(UserSettings.TimeFormat.values()).map(Enum::name).toList())
                .chartPeriods(Arrays.stream(UserSettings.ChartPeriod.values()).map(Enum::name).toList())
                .build();

        return UserSettingsResponse.builder()
                .id(settings.getId())
                .currencyPreference(settings.getCurrencyPreference())
                .currencySymbol(currencyConverter.getCurrencySymbol(settings.getCurrencyPreference()))
                .timezone(settings.getTimezone())
                .dateFormat(settings.getDateFormat())
                .formattedDateExample(settings.getFormattedDateExample())
                .timeFormat(settings.getTimeFormat())
                .formattedTimeExample(settings.getFormattedTimeExample())
                .theme(settings.getTheme())
                .language(settings.getLanguage())
                .emailNotifications(settings.getEmailNotifications())
                .budgetAlerts(settings.getBudgetAlerts())
                .usageAlerts(settings.getUsageAlerts())
                .weeklyReports(settings.getWeeklyReports())
                .marketingEmails(settings.getMarketingEmails())
                .hasNotificationsEnabled(settings.hasNotificationsEnabled())
                .itemsPerPage(settings.getItemsPerPage())
                .defaultChartPeriod(settings.getDefaultChartPeriod())
                .autoRefreshDashboard(settings.getAutoRefreshDashboard())
                .dashboardRefreshInterval(settings.getDashboardRefreshInterval())
                .dataSharing(settings.getDataSharing())
                .analyticsCookies(settings.getAnalyticsCookies())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .availableOptions(options)
                .build();
    }

    private List<String> getAvailableTimezones() {
        return List.of(
                "UTC",
                "America/New_York",
                "America/Chicago",
                "America/Denver",
                "America/Los_Angeles",
                "Europe/London",
                "Europe/Paris",
                "Europe/Berlin",
                "Asia/Tokyo",
                "Asia/Shanghai",
                "Asia/Kolkata",
                "Asia/Dubai",
                "Australia/Sydney"
        );
    }

    private List<String> getAvailableDateFormats() {
        return List.of(
                "MM/dd/yyyy",
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "dd MMM yyyy"
        );
    }

    private List<String> getAvailableLanguages() {
        return List.of(
                "en", // English
                "es", // Spanish
                "fr", // French
                "de", // German
                "it", // Italian
                "pt", // Portuguese
                "ru", // Russian
                "zh", // Chinese
                "ja", // Japanese
                "hi"  // Hindi
        );
    }

    private <T> void updateFieldIfNotNull(java.util.function.Consumer<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
