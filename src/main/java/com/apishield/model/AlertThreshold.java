package com.apishield.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_thresholds",
        uniqueConstraints = @UniqueConstraint(columnNames = {"api_service_id"}))
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertThreshold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_service_id", nullable = false, unique = true)
    private ApiService apiService;

    @Column(name = "warning_percent", nullable = false)
    private Double warningPercent; // Budget utilization warning threshold (e.g., 75%)

    @Column(name = "critical_percent", nullable = false)
    private Double criticalPercent; // Budget utilization critical threshold (e.g., 90%)

    @Column(name = "spike_threshold", nullable = false)
    private Double spikeThreshold; // Usage spike threshold percentage (e.g., 50%)

    @Column(name = "error_threshold", nullable = false)
    private Double errorThreshold; // Error rate threshold (e.g., 0.1 for 10%)

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    @Transient
    public boolean shouldTriggerWarning(double utilizationPercent) {
        return isEnabled && utilizationPercent >= warningPercent;
    }

    @Transient
    public boolean shouldTriggerCritical(double utilizationPercent) {
        return isEnabled && utilizationPercent >= criticalPercent;
    }

    @Transient
    public boolean shouldTriggerSpike(double spikePercent) {
        return isEnabled && Math.abs(spikePercent) >= spikeThreshold;
    }

    @Transient
    public boolean shouldTriggerError(double errorRate) {
        return isEnabled && errorRate >= errorThreshold;
    }

    // Validation
    @PrePersist
    @PreUpdate
    private void validateThresholds() {
        if (warningPercent >= criticalPercent) {
            throw new IllegalArgumentException("Warning threshold must be less than critical threshold");
        }
        if (warningPercent < 0 || criticalPercent < 0 || spikeThreshold < 0 || errorThreshold < 0) {
            throw new IllegalArgumentException("Threshold values cannot be negative");
        }
        if (warningPercent > 100 || criticalPercent > 100) {
            throw new IllegalArgumentException("Budget threshold percentages cannot exceed 100");
        }
        if (errorThreshold > 1.0) {
            throw new IllegalArgumentException("Error threshold cannot exceed 1.0 (100%)");
        }
    }
}
