package com.apishield.dto.settings;

import com.apishield.model.User;
import com.apishield.model.UserSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {
    private Long id;
    private User.Currency currencyPreference;
    private String currencySymbol;
    private String timezone;
    private String dateFormat;
    private String formattedDateExample;
    private UserSettings.TimeFormat timeFormat;
    private String formattedTimeExample;
    private UserSettings.Theme theme;
    private String language;

    // Notification preferences
    private Boolean emailNotifications;
    private Boolean budgetAlerts;
    private Boolean usageAlerts;
    private Boolean weeklyReports;
    private Boolean marketingEmails;
    private Boolean hasNotificationsEnabled;

    // Display preferences
    private Integer itemsPerPage;
    private UserSettings.ChartPeriod defaultChartPeriod;
    private Boolean autoRefreshDashboard;
    private Integer dashboardRefreshInterval;

    // Privacy preferences
    private Boolean dataSharing;
    private Boolean analyticsCookies;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Available options for dropdowns
    private AvailableOptions availableOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableOptions {
        private List<String> timezones;
        private List<String> dateFormats;
        private List<String> languages;
        private List<String> themes;
        private List<String> timeFormats;
        private List<String> chartPeriods;
    }
}
