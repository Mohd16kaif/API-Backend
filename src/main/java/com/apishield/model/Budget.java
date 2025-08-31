package com.apishield.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "budgets")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "monthly_budget", nullable = false)
    private Double monthlyBudget;

    @Column(name = "spent_amount", nullable = false)
    private Double spentAmount = 0.0;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods for calculations
    @Transient
    public double getRemainingBudget() {
        return Math.max(0.0, monthlyBudget - spentAmount);
    }

    @Transient
    public double getUtilizationPercentage() {
        if (monthlyBudget == null || monthlyBudget == 0.0) {
            return 0.0;
        }
        return Math.min(100.0, (spentAmount / monthlyBudget) * 100.0);
    }

    @Transient
    public boolean isOverBudget() {
        return spentAmount > monthlyBudget;
    }

    @Transient
    public String getStatus() {
        double utilization = getUtilizationPercentage();
        if (utilization >= 100.0) {
            return "over_budget";
        } else if (utilization >= 90.0) {
            return "critical";
        } else if (utilization >= 75.0) {
            return "warning";
        } else {
            return "healthy";
        }
    }
}
