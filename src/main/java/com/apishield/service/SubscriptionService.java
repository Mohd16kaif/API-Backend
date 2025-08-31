package com.apishield.service;

import com.apishield.dto.plan.*;
import com.apishield.exception.BadRequestException;
import com.apishield.exception.ResourceNotFoundException;
import com.apishield.model.*;
import com.apishield.repository.*;
import com.apishield.util.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PaymentProcessor paymentProcessor;

    @Transactional
    public SubscriptionResponse subscribe(User user, SubscribeRequest request) {
        log.info("Processing subscription for user: {} to plan: {}", user.getEmail(), request.getPlanId());

        // Validate plan
        SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));

        if (!plan.getIsActive()) {
            throw new BadRequestException("This subscription plan is no longer available");
        }

        // Check if user has active subscription
        Optional<UserSubscription> existingSubscription = subscriptionRepository.findActiveSubscription(user);

        // Calculate amount to charge
        Double amount = plan.getPriceForCurrency(user.getCurrencyPreference());

        // Process payment (only if not free plan)
        String paymentReference = null;
        String paymentDetails = null;

        if (amount > 0) {
            PaymentProcessor.PaymentRequest paymentRequest = PaymentProcessor.PaymentRequest.builder()
                    .amount(amount)
                    .currency(user.getCurrencyPreference().name())
                    .paymentMode(request.getPaymentMode())
                    .customerEmail(user.getEmail())
                    .description("Subscription to " + plan.getName() + " plan")
                    .metadata(Map.of(
                            "userId", user.getId(),
                            "planId", plan.getId(),
                            "planName", plan.getName()
                    ))
                    .build();

            PaymentProcessor.PaymentResult paymentResult = paymentProcessor.processPayment(paymentRequest);

            if (!paymentResult.getSuccess()) {
                log.error("Payment failed for user: {} - {}", user.getEmail(), paymentResult.getMessage());
                throw new BadRequestException("Payment failed: " + paymentResult.getMessage());
            }

            paymentReference = paymentResult.getTransactionId();
            paymentDetails = convertMapToJson(paymentResult.getPaymentDetails());
        } else {
            paymentReference = "FREE_PLAN_" + System.currentTimeMillis();
        }

        // Cancel existing subscription if upgrading/downgrading
        if (existingSubscription.isPresent()) {
            UserSubscription existing = existingSubscription.get();
            existing.cancel("Upgraded to " + plan.getName() + " plan");
            subscriptionRepository.save(existing);
            log.info("Cancelled existing subscription for user: {}", user.getEmail());
        }

        // Create new subscription
        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .paymentMode(request.getPaymentMode())
                .status(UserSubscription.Status.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .amountPaid(amount)
                .currencyPaid(user.getCurrencyPreference())
                .paymentReference(paymentReference)
                .paymentDetails(paymentDetails)
                .autoRenew(request.getAutoRenew())
                .build();

        UserSubscription savedSubscription = subscriptionRepository.save(subscription);
        log.info("Successfully created subscription ID: {} for user: {}", savedSubscription.getId(), user.getEmail());

        return mapToSubscriptionResponse(savedSubscription);
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionResponse> getCurrentSubscription(User user) {
        log.info("Fetching current subscription for user: {}", user.getEmail());

        return subscriptionRepository.findActiveSubscription(user)
                .map(this::mapToSubscriptionResponse);
    }

    @Transactional(readOnly = true)
    public BillingHistoryResponse getBillingHistory(User user) {
        log.info("Fetching billing history for user: {}", user.getEmail());

        List<UserSubscription> subscriptions = subscriptionRepository.findBillingHistory(user);
        List<SubscriptionResponse> subscriptionResponses = subscriptions.stream()
                .map(this::mapToSubscriptionResponse)
                .collect(Collectors.toList());

        // Calculate statistics
        double totalAmount = subscriptions.stream()
                .filter(s -> s.getAmountPaid() != null)
                .mapToDouble(UserSubscription::getAmountPaid)
                .sum();

        Optional<UserSubscription> currentSub = subscriptionRepository.findActiveSubscription(user);
        String currentPlan = currentSub.map(s -> s.getPlan().getName()).orElse("None");

        LocalDate firstSubscriptionDate = subscriptions.stream()
                .map(UserSubscription::getStartDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastPaymentDate = subscriptions.stream()
                .map(UserSubscription::getCreatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(dateTime -> dateTime.toLocalDate())
                .orElse(null);

        // Generate billing stats
        BillingHistoryResponse.BillingStats stats = generateBillingStats(subscriptions);

        String totalFormatted = String.format("%s%.2f",
                user.getCurrencyPreference() == User.Currency.USD ? "$" : "₹",
                totalAmount);

        return BillingHistoryResponse.builder()
                .subscriptions(subscriptionResponses)
                .totalAmountPaid(totalAmount)
                .totalAmountFormatted(totalFormatted)
                .totalSubscriptions(subscriptions.size())
                .firstSubscriptionDate(firstSubscriptionDate)
                .lastPaymentDate(lastPaymentDate)
                .currentPlan(currentPlan)
                .hasActiveSubscription(currentSub.isPresent())
                .stats(stats)
                .build();
    }

    @Transactional
    public SubscriptionResponse cancelSubscription(User user, String reason) {
        log.info("Cancelling subscription for user: {} - Reason: {}", user.getEmail(), reason);

        UserSubscription subscription = subscriptionRepository.findActiveSubscription(user)
                .orElseThrow(() -> new BadRequestException("No active subscription found"));

        subscription.cancel(reason);
        UserSubscription savedSubscription = subscriptionRepository.save(subscription);

        return mapToSubscriptionResponse(savedSubscription);
    }

    @Transactional
    public SubscriptionResponse renewSubscription(User user) {
        log.info("Renewing subscription for user: {}", user.getEmail());

        UserSubscription currentSubscription = subscriptionRepository.findActiveSubscription(user)
                .orElseThrow(() -> new BadRequestException("No active subscription found"));

        if (currentSubscription.getStatus() != UserSubscription.Status.ACTIVE) {
            throw new BadRequestException("Cannot renew inactive subscription");
        }

        // Process payment for renewal
        SubscriptionPlan plan = currentSubscription.getPlan();
        Double amount = plan.getPriceForCurrency(user.getCurrencyPreference());

        if (amount > 0) {
            PaymentProcessor.PaymentRequest paymentRequest = PaymentProcessor.PaymentRequest.builder()
                    .amount(amount)
                    .currency(user.getCurrencyPreference().name())
                    .paymentMode(currentSubscription.getPaymentMode())
                    .customerEmail(user.getEmail())
                    .description("Renewal of " + plan.getName() + " plan")
                    .build();

            PaymentProcessor.PaymentResult paymentResult = paymentProcessor.processPayment(paymentRequest);

            if (!paymentResult.getSuccess()) {
                throw new BadRequestException("Renewal payment failed: " + paymentResult.getMessage());
            }
        }

        // Extend subscription
        currentSubscription.setEndDate(currentSubscription.getEndDate().plusMonths(1));
        UserSubscription savedSubscription = subscriptionRepository.save(currentSubscription);

        return mapToSubscriptionResponse(savedSubscription);
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getBillingHistory(User user, Pageable pageable) {
        log.info("Fetching paginated billing history for user: {}", user.getEmail());

        Page<UserSubscription> subscriptions = subscriptionRepository
                .findByUserOrderByCreatedAtDesc(user, pageable);

        return subscriptions.map(this::mapToSubscriptionResponse);
    }

    /**
     * Check and process subscription renewals (for scheduler)
     */
    @Transactional
    public void processSubscriptionRenewals() {
        log.info("Processing subscription renewals...");

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<UserSubscription> expiringSubscriptions = subscriptionRepository
                .findSubscriptionsExpiringOn(tomorrow);

        for (UserSubscription subscription : expiringSubscriptions) {
            if (subscription.getAutoRenew() && subscription.getStatus() == UserSubscription.Status.ACTIVE) {
                try {
                    renewSubscription(subscription.getUser());
                    log.info("Auto-renewed subscription for user: {}", subscription.getUser().getEmail());
                } catch (Exception e) {
                    log.error("Failed to auto-renew subscription for user: {}",
                            subscription.getUser().getEmail(), e);
                    // Could send notification about failed renewal
                }
            }
        }
    }

    /**
     * Mark expired subscriptions
     */
    @Transactional
    public void markExpiredSubscriptions() {
        LocalDate today = LocalDate.now();
        List<UserSubscription> expiredSubscriptions = subscriptionRepository
                .findSubscriptionsExpiringOn(today.minusDays(1));

        for (UserSubscription subscription : expiredSubscriptions) {
            if (subscription.getStatus() == UserSubscription.Status.ACTIVE) {
                subscription.expire();
                subscriptionRepository.save(subscription);
                log.info("Marked subscription as expired for user: {}", subscription.getUser().getEmail());
            }
        }
    }

    private SubscriptionResponse mapToSubscriptionResponse(UserSubscription subscription) {
        SubscriptionPlan plan = subscription.getPlan();

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .planId(plan.getId())
                .planName(plan.getName())
                .paymentMode(subscription.getPaymentMode())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .formattedAmountPaid(subscription.getFormattedAmountPaid())
                .currencyPaid(subscription.getCurrencyPaid())
                .paymentReference(subscription.getPaymentReference())
                .autoRenew(subscription.getAutoRenew())
                .createdAt(subscription.getCreatedAt())
                .isActive(subscription.isActive())
                .isExpired(subscription.isExpired())
                .daysRemaining(subscription.getDaysRemaining())
                .utilizationPercentage(subscription.getUtilizationPercentage())
                .statusDisplay(formatStatusDisplay(subscription.getStatus()))
                .statusColor(getStatusColor(subscription.getStatus()))
                .maxApis(plan.getMaxApis())
                .maxRequestsPerMonth(plan.getMaxRequestsPerMonth())
                .supportLevel(plan.getSupportLevel())
                .canRenew(canRenew(subscription))
                .canCancel(canCancel(subscription))
                .canUpgrade(canUpgrade(subscription))
                .nextBillingDate(subscription.getEndDate() != null ? subscription.getEndDate().toString() : null)
                .build();
    }

    private String formatStatusDisplay(UserSubscription.Status status) {
        return switch (status) {
            case ACTIVE -> "Active";
            case EXPIRED -> "Expired";
            case CANCELLED -> "Cancelled";
            case PENDING -> "Pending Payment";
            case FAILED -> "Payment Failed";
        };
    }

    private String getStatusColor(UserSubscription.Status status) {
        return switch (status) {
            case ACTIVE -> "#28a745";      // Green
            case EXPIRED -> "#6c757d";     // Gray
            case CANCELLED -> "#dc3545";   // Red
            case PENDING -> "#ffc107";     // Yellow
            case FAILED -> "#dc3545";      // Red
        };
    }

    private boolean canRenew(UserSubscription subscription) {
        return subscription.getStatus() == UserSubscription.Status.ACTIVE ||
                subscription.getStatus() == UserSubscription.Status.EXPIRED;
    }

    private boolean canCancel(UserSubscription subscription) {
        return subscription.getStatus() == UserSubscription.Status.ACTIVE;
    }

    private boolean canUpgrade(UserSubscription subscription) {
        return subscription.getStatus() == UserSubscription.Status.ACTIVE;
    }

    private BillingHistoryResponse.BillingStats generateBillingStats(List<UserSubscription> subscriptions) {
        if (subscriptions.isEmpty()) {
            return BillingHistoryResponse.BillingStats.builder()
                    .averageMonthlySpend(0.0)
                    .subscriptionChanges(0)
                    .totalActiveMonths(0L)
                    .mostUsedPaymentMode("None")
                    .preferredCurrency("None")
                    .build();
        }

        // Calculate average monthly spend
        double totalSpent = subscriptions.stream()
                .filter(s -> s.getAmountPaid() != null)
                .mapToDouble(UserSubscription::getAmountPaid)
                .sum();

        long totalMonths = subscriptions.stream()
                .filter(s -> s.getStartDate() != null && s.getEndDate() != null)
                .mapToLong(s -> java.time.temporal.ChronoUnit.MONTHS.between(s.getStartDate(), s.getEndDate()))
                .sum();

        double averageMonthlySpend = totalMonths > 0 ? totalSpent / totalMonths : 0.0;

        // Most used payment mode
        String mostUsedPaymentMode = subscriptions.stream()
                .collect(Collectors.groupingBy(UserSubscription::getPaymentMode, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().name())
                .orElse("None");

        // Preferred currency
        String preferredCurrency = subscriptions.stream()
                .filter(s -> s.getCurrencyPaid() != null)
                .collect(Collectors.groupingBy(UserSubscription::getCurrencyPaid, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().name())
                .orElse("None");

        return BillingHistoryResponse.BillingStats.builder()
                .averageMonthlySpend(Math.round(averageMonthlySpend * 100.0) / 100.0)
                .subscriptionChanges(subscriptions.size() - 1) // Number of plan changes
                .totalActiveMonths(totalMonths)
                .mostUsedPaymentMode(mostUsedPaymentMode)
                .preferredCurrency(preferredCurrency)
                .build();
    }

    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";

        try {
            // Simple JSON conversion - in production use ObjectMapper
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":\"")
                        .append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
