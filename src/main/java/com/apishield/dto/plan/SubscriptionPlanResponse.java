package com.apishield.dto.plan;

import com.apishield.model.SubscriptionPlan;
import com.apishield.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanResponse {
    private Long id;
    private String name;
    private Double priceUsd;
    private Double priceInr;
    private String formattedPrice;
    private String currencySymbol;
    private List<String> features;
    private Integer maxApis;
    private Long maxRequestsPerMonth;
    private SubscriptionPlan.SupportLevel supportLevel;
    private String supportLevelDisplay;
    private Boolean isActive;
    private Integer displayOrder;
    private Boolean isFreePlan;
    private Boolean isCurrentPlan;
    private Boolean isUpgrade;
    private Boolean isDowngrade;

    // Plan comparison fields
    private String recommendation; // "Most Popular", "Best Value", etc.
    private List<String> highlights; // Key selling points
}
