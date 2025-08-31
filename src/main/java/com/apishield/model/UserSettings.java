package com.apishield.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "user_settings")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_preference", nullable = false)
    private User.Currency currencyPreference = User.Currency.USD;

    @Column(name = "timezone")
    private String timezone = "UTC"; // e.g., "America/New_York", "Asia/Kolkata"

    @Column(name = "date_format")
    private String dateFormat = "MM/dd/yyyy"; // US format by default

    @Column(name = "time_format")
    @Enumerated(EnumType.STRING)
    private TimeFormat timeFormat = TimeFormat.TWELVE_HOUR;

    @Column(name = "theme")
    @Enumerated(EnumType.STRING)
    private Theme theme = Theme.LIGHT;

    @Column(name = "language")
    private String language = "en"; // ISO 639-1 language code

    // Notification preferences
    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;

    @Column(name = "budget_alerts")
    private Boolean budgetAlerts = true;

    @Column(name = "usage_alerts")
    private Boolean usageAlerts = true;

    @Column(name = "weekly_reports")
    private Boolean weeklyReports = true;

    @Column(name = "marketing_emails")
    private Boolean marketingEmails = false;

    // Display preferences
    @Column(name = "items_per_page")
    private Integer itemsPerPage = 20;

    @Column(name = "default_chart_period")
    @Enumerated(EnumType.STRING)
    private ChartPeriod defaultChartPeriod = ChartPeriod.LAST_30_DAYS;

    @Column(name = "auto_refresh_dashboard")
    private Boolean autoRefreshDashboard = true;

    @Column(name = "dashboard_refresh_interval")
    private Integer dashboardRefreshInterval = 300; // seconds

    // Privacy preferences
    @Column(name = "data_sharing")
    private Boolean dataSharing = false;

    @Column(name = "analytics_cookies")
    private Boolean analyticsCookies = true;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TimeFormat {
        TWELVE_HOUR, // 12:30 PM
        TWENTY_FOUR_HOUR // 12:30
    }

    public enum Theme {
        LIGHT,
        DARK,
        AUTO // System preference
    }

    public enum ChartPeriod {
        LAST_7_DAYS,
        LAST_30_DAYS,
        LAST_90_DAYS,
        LAST_YEAR,
        ALL_TIME
    }

    // Helper methods
    @Transient
    public ZoneId getZoneId() {
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }

    @Transient
    public String getFormattedTimeExample() {
        return timeFormat == TimeFormat.TWELVE_HOUR ? "12:30 PM" : "12:30";
    }

    @Transient
    public String getFormattedDateExample() {
        return switch (dateFormat) {
            case "dd/MM/yyyy" -> "31/12/2023";
            case "yyyy-MM-dd" -> "2023-12-31";
            case "dd MMM yyyy" -> "31 Dec 2023";
            default -> "12/31/2023"; // MM/dd/yyyy
        };
    }

    @Transient
    public boolean hasNotificationsEnabled() {
        return emailNotifications && (budgetAlerts || usageAlerts || weeklyReports);
    }
}
