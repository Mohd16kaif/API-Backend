package com.apishield.dto.settings;

import com.apishield.model.User;
import com.apishield.model.UserSettings;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserSettingsRequest {

    private User.Currency currencyPreference;

    @Pattern(regexp = "^[A-Za-z_]+/[A-Za-z_]+$", message = "Invalid timezone format")
    private String timezone;

    @Pattern(regexp = "^(dd/MM/yyyy|MM/dd/yyyy|yyyy-MM-dd|dd MMM yyyy)$",
            message = "Invalid date format")
    private String dateFormat;

    private UserSettings.TimeFormat timeFormat;

    private UserSettings.Theme theme;

    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Invalid language code")
    private String language;

    // Notification preferences
    private Boolean emailNotifications;
    private Boolean budgetAlerts;
    private Boolean usageAlerts;
    private Boolean weeklyReports;
    private Boolean marketingEmails;

    // Display preferences
    @Min(value = 5, message = "Items per page must be at least 5")
    @Max(value = 100, message = "Items per page cannot exceed 100")
    private Integer itemsPerPage;

    private UserSettings.ChartPeriod defaultChartPeriod;

    private Boolean autoRefreshDashboard;

    @Min(value = 30, message = "Refresh interval must be at least 30 seconds")
    @Max(value = 3600, message = "Refresh interval cannot exceed 1 hour")
    private Integer dashboardRefreshInterval;

    // Privacy preferences
    private Boolean dataSharing;
    private Boolean analyticsCookies;
}
