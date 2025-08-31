package com.apishield.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_service_id")
    private ApiService apiService;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(name = "threshold_value")
    private Double thresholdValue; // The threshold that was exceeded

    @Column(name = "actual_value")
    private Double actualValue; // The actual value that triggered the alert

    @Column(name = "is_resolved")
    private Boolean isResolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum AlertType {
        BUDGET_WARNING,
        BUDGET_CRITICAL,
        USAGE_SPIKE,
        HIGH_ERROR_RATE,
        SERVICE_DOWN,
        COST_ANOMALY
    }

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    // Helper methods
    @Transient
    public boolean isCritical() {
        return severity == Severity.CRITICAL || severity == Severity.HIGH;
    }

    @Transient
    public String getFormattedMessage() {
        if (apiService != null) {
            return String.format("[%s] %s", apiService.getName(), message);
        }
        return message;
    }

    @Transient
    public long getMinutesSinceCreated() {
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }

    public void resolve() {
        this.isResolved = true;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markNotificationSent() {
        this.notificationSent = true;
    }
}
