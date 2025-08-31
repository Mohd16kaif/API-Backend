package com.apishield.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "subscription_plans")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "price_usd", nullable = false)
    private Double priceUsd;

    @Column(name = "price_inr", nullable = false)
    private Double priceInr;

    @Column(columnDefinition = "TEXT")
    private String features; // JSON string or comma-separated

    @Column(name = "max_apis")
    private Integer maxApis; // Maximum number of API services allowed

    @Column(name = "max_api_services") // ADD THIS - maps to your MySQL column
    private Integer maxApiServices; // Alternative/legacy field name

    @Column(name = "max_requests_per_month")
    private Long maxRequestsPerMonth; // Request limit per month

    @Column(name = "support_level")
    @Enumerated(EnumType.STRING)
    private SupportLevel supportLevel;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "display_order") // ADD THIS - maps to your MySQL column
    private Integer displayOrder = 0;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate // ADD THIS for proper auditing
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SupportLevel {
        BASIC,
        PRIORITY,
        DEDICATED
    }

    // Helper methods
    @Transient
    public Double getPriceForCurrency(User.Currency currency) {
        return currency == User.Currency.USD ? priceUsd : priceInr;
    }

    @Transient
    public String getCurrencySymbol(User.Currency currency) {
        return currency == User.Currency.USD ? "$" : "₹";
    }

    @Transient
    public List<String> getFeatureList() {
        if (features == null || features.trim().isEmpty()) {
            return List.of();
        }
        return List.of(features.split(","));
    }

    @Transient
    public boolean isFreePlan() {
        return "Starter".equalsIgnoreCase(name) && (priceUsd == 0.0 || priceInr == 0.0);
    }

    @Transient
    public String getFormattedPrice(User.Currency currency) {
        Double price = getPriceForCurrency(currency);
        String symbol = getCurrencySymbol(currency);
        return String.format("%s%.2f/month", symbol, price);
    }

    // Get the effective max API services (use maxApiServices if available, otherwise maxApis)
    @Transient
    public Integer getEffectiveMaxApiServices() {
        return maxApiServices != null ? maxApiServices : maxApis;
    }
}