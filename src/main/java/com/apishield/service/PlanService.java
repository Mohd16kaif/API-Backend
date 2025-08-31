package com.apishield.service;

import com.apishield.dto.plan.SubscriptionPlanResponse;
import com.apishield.model.SubscriptionPlan;
import com.apishield.model.User;
import com.apishield.model.UserSubscription;
import com.apishield.repository.SubscriptionPlanRepository;
import com.apishield.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanService {

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAllPlans(User user) {
        log.info("Fetching all active plans for user: {}", user.getEmail());

        List<SubscriptionPlan> plans = planRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        Optional<UserSubscription> currentSubscription = subscriptionRepository.findActiveSubscription(user);

        return plans.stream()
                .map(plan -> mapToPlanResponse(plan, user, currentSubscription.orElse(null)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanById(Long planId, User user) {
        log.info("Fetching plan ID: {} for user: {}", planId, user.getEmail());

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        Optional<UserSubscription> currentSubscription = subscriptionRepository.findActiveSubscription(user);

        return mapToPlanResponse(plan, user, currentSubscription.orElse(null));
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getRecommendedPlans(User user) {
        log.info("Getting plan recommendations for user: {}", user.getEmail());

        List<SubscriptionPlan> allPlans = planRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        Optional<UserSubscription> currentSubscription = subscriptionRepository.findActiveSubscription(user);

        // Logic to recommend plans based on user's current usage, API count, etc.
        // For now, return all plans with recommendations
        return allPlans.stream()
                .map(plan -> {
                    SubscriptionPlanResponse response = mapToPlanResponse(plan, user, currentSubscription.orElse(null));
                    response.setRecommendation(generateRecommendation(plan, user));
                    response.setHighlights(generateHighlights(plan));
                    return response;
                })
                .collect(Collectors.toList());
    }

    private SubscriptionPlanResponse mapToPlanResponse(SubscriptionPlan plan, User user, UserSubscription currentSubscription) {
        User.Currency userCurrency = user.getCurrencyPreference();
        Double planPrice = plan.getPriceForCurrency(userCurrency);
        String formattedPrice = plan.getFormattedPrice(userCurrency);
        String currencySymbol = plan.getCurrencySymbol(userCurrency);

        boolean isCurrentPlan = currentSubscription != null &&
                currentSubscription.getPlan().getId().equals(plan.getId());

        boolean isUpgrade = false;
        boolean isDowngrade = false;

        if (currentSubscription != null && !isCurrentPlan) {
            Double currentPlanPrice = currentSubscription.getPlan().getPriceForCurrency(userCurrency);
            isUpgrade = planPrice > currentPlanPrice;
            isDowngrade = planPrice < currentPlanPrice;
        }

        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .priceUsd(plan.getPriceUsd())
                .priceInr(plan.getPriceInr())
                .formattedPrice(formattedPrice)
                .currencySymbol(currencySymbol)
                .features(plan.getFeatureList())
                .maxApis(plan.getMaxApis())
                .maxRequestsPerMonth(plan.getMaxRequestsPerMonth())
                .supportLevel(plan.getSupportLevel())
                .supportLevelDisplay(formatSupportLevel(plan.getSupportLevel()))
                .isActive(plan.getIsActive())
                .displayOrder(plan.getDisplayOrder())
                .isFreePlan(plan.isFreePlan())
                .isCurrentPlan(isCurrentPlan)
                .isUpgrade(isUpgrade)
                .isDowngrade(isDowngrade)
                .build();
    }

    private String formatSupportLevel(SubscriptionPlan.SupportLevel supportLevel) {
        return switch (supportLevel) {
            case BASIC -> "Community Support";
            case PRIORITY -> "Priority Support";
            case DEDICATED -> "Dedicated Support Manager";
        };
    }

    private String generateRecommendation(SubscriptionPlan plan, User user) {
        return switch (plan.getName().toLowerCase()) {
            case "starter" -> plan.isFreePlan() ? "Perfect for getting started" : "Great for small projects";
            case "pro" -> "Most Popular - Best for growing businesses";
            case "enterprise" -> "Advanced features for large teams";
            default -> null;
        };
    }

    private List<String> generateHighlights(SubscriptionPlan plan) {
        List<String> highlights = new ArrayList<>();

        if (plan.getMaxApis() != null) {
            if (plan.getMaxApis() == -1) {
                highlights.add("Unlimited API services");
            } else {
                highlights.add(plan.getMaxApis() + " API services included");
            }
        }

        if (plan.getMaxRequestsPerMonth() != null) {
            if (plan.getMaxRequestsPerMonth() == -1) {
                highlights.add("Unlimited API requests");
            } else {
                highlights.add(formatNumber(plan.getMaxRequestsPerMonth()) + " requests/month");
            }
        }

        if (plan.getSupportLevel() == SubscriptionPlan.SupportLevel.DEDICATED) {
            highlights.add("24/7 dedicated support");
        } else if (plan.getSupportLevel() == SubscriptionPlan.SupportLevel.PRIORITY) {
            highlights.add("Priority customer support");
        }

        return highlights;
    }

    private String formatNumber(Long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return number.toString();
        }
    }
}
