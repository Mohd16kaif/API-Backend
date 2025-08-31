package com.apishield.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "usage_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"api_service_id", "log_date"}))
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_service_id", nullable = false)
    private ApiService apiService;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "requests_made", nullable = false)
    private Integer requestsMade;

    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Column(name = "peak_hour", nullable = false)
    private Integer peakHour; // 0-23

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Helper methods for calculations
    @Transient
    public double getSuccessRate() {
        if (requestsMade == null || requestsMade == 0) {
            return 0.0;
        }
        return Math.round((successCount.doubleValue() / requestsMade.doubleValue()) * 10000.0) / 100.0;
    }

    @Transient
    public double getErrorRate() {
        if (requestsMade == null || requestsMade == 0) {
            return 0.0;
        }
        return Math.round((errorCount.doubleValue() / requestsMade.doubleValue()) * 10000.0) / 100.0;
    }

    @Transient
    public boolean isHighErrorRate() {
        return getErrorRate() > 5.0; // More than 5% error rate
    }

    @Transient
    public String getPeakHourFormatted() {
        return String.format("%02d:00", peakHour);
    }

    // Validation
    @PrePersist
    @PreUpdate
    private void validateData() {
        if (peakHour < 0 || peakHour > 23) {
            throw new IllegalArgumentException("Peak hour must be between 0 and 23");
        }
        if (successCount + errorCount > requestsMade) {
            throw new IllegalArgumentException("Success + Error count cannot exceed total requests");
        }
        if (successCount < 0 || errorCount < 0 || requestsMade < 0) {
            throw new IllegalArgumentException("Counts cannot be negative");
        }
    }
}
