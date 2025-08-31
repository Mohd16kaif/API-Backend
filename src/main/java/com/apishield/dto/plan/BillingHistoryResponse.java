package com.apishield.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingHistoryResponse {
    private List<SubscriptionResponse> subscriptions;
    private Double totalAmountPaid;
    private String totalAmountFormatted;
    private Integer totalSubscriptions;
    private LocalDate firstSubscriptionDate;
    private LocalDate lastPaymentDate;
    private String currentPlan;
    private Boolean hasActiveSubscription;

    // Statistics
    private BillingStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingStats {
        private Double averageMonthlySpend;
        private Integer subscriptionChanges; // Number of plan changes
        private Long totalActiveMonths;
        private String mostUsedPaymentMode;
        private String preferredCurrency;
    }
}
