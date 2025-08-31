package com.apishield.dto.plan;

import com.apishield.model.SubscriptionPlan;
import com.apishield.model.UserSubscription;
import com.apishield.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private Long id;
    private Long planId;
    private String planName;
    private UserSubscription.PaymentMode paymentMode;
    private UserSubscription.Status status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String formattedAmountPaid;
    private User.Currency currencyPaid;
    private String paymentReference;
    private Boolean autoRenew;
    private LocalDateTime createdAt;

    // Calculated fields
    private Boolean isActive;
    private Boolean isExpired;
    private Long daysRemaining;
    private Double utilizationPercentage;
    private String statusDisplay;
    private String statusColor; // For UI styling

    // Plan details for convenience
    private Integer maxApis;
    private Long maxRequestsPerMonth;
    private SubscriptionPlan.SupportLevel supportLevel;

    // Renewal information
    private Boolean canRenew;
    private Boolean canCancel;
    private Boolean canUpgrade;
    private String nextBillingDate;
}
