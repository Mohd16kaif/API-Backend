package com.apishield.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_services")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "endpoint_url", nullable = false, length = 512)
    private String endpointUrl;

    @Column(nullable = false)
    private Double budget;

    @Column(name = "cost_per_unit", nullable = false)
    private Double costPerUnit;

    @Column(name = "usage_count", nullable = false)
    private Double usageCount = 0.0;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper method to calculate utilization percentage
    @Transient
    public double getUtilizationPercentage() {
        if (budget == null || budget == 0.0) {
            return 0.0;
        }
        double totalCost = usageCount * costPerUnit;
        return Math.min(100.0, (totalCost / budget) * 100.0);
    }

    // Helper method to get remaining budget
    @Transient
    public double getRemainingBudget() {
        double totalCost = usageCount * costPerUnit;
        return Math.max(0.0, budget - totalCost);
    }

    // Helper method to get total spent
    @Transient
    public double getTotalSpent() {
        return usageCount * costPerUnit;
    }
}