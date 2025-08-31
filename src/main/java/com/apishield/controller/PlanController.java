package com.apishield.controller;

import com.apishield.dto.plan.*;
import com.apishield.model.User;
import com.apishield.model.UserSubscription;
import com.apishield.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Plans & Billing", description = "Subscription plans and billing management APIs")
public class PlanController {

    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private final PaymentService paymentService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all available subscription plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getAllPlans(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        List<SubscriptionPlanResponse> plans = planService.getAllPlans(user);
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific subscription plan details")
    public ResponseEntity<SubscriptionPlanResponse> getPlanById(
            Authentication authentication,
            @Parameter(description = "Plan ID") @PathVariable Long id) {
        User user = userService.getCurrentUserEntity(authentication);
        SubscriptionPlanResponse plan = planService.getPlanById(id, user);
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get personalized plan recommendations")
    public ResponseEntity<List<SubscriptionPlanResponse>> getRecommendedPlans(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        List<SubscriptionPlanResponse> plans = planService.getRecommendedPlans(user);
        return ResponseEntity.ok(plans);
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to a plan")
    public ResponseEntity<SubscriptionResponse> subscribe(
            Authentication authentication,
            @Valid @RequestBody SubscribeRequest request) {
        User user = userService.getCurrentUserEntity(authentication);
        SubscriptionResponse response = subscriptionService.subscribe(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/current")
    @Operation(summary = "Get current active subscription")
    public ResponseEntity<SubscriptionResponse> getCurrentSubscription(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        Optional<SubscriptionResponse> subscription = subscriptionService.getCurrentSubscription(user);

        return subscription.map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel current subscription")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        User user = userService.getCurrentUserEntity(authentication);
        String reason = request.getOrDefault("reason", "User requested cancellation");
        SubscriptionResponse response = subscriptionService.cancelSubscription(user, reason);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/renew")
    @Operation(summary = "Renew current subscription")
    public ResponseEntity<SubscriptionResponse> renewSubscription(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        SubscriptionResponse response = subscriptionService.renewSubscription(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/billing/history")
    @Operation(summary = "Get billing history")
    public ResponseEntity<BillingHistoryResponse> getBillingHistory(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        BillingHistoryResponse history = subscriptionService.getBillingHistory(user);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/billing/history/paginated")
    @Operation(summary = "Get paginated billing history")
    public ResponseEntity<Page<SubscriptionResponse>> getBillingHistoryPaginated(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = userService.getCurrentUserEntity(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<SubscriptionResponse> history = subscriptionService.getBillingHistory(user, pageable);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "Get available payment methods")
    public ResponseEntity<Map<String, Object>> getPaymentMethods(
            @RequestParam UserSubscription.PaymentMode paymentMode) {
        Map<String, Object> methods = paymentService.getPaymentMethods(paymentMode);
        return ResponseEntity.ok(methods);
    }

    @GetMapping("/payment-methods/supported")
    @Operation(summary = "Check if payment method supports currency")
    public ResponseEntity<Map<String, Boolean>> isPaymentMethodSupported(
            @RequestParam UserSubscription.PaymentMode paymentMode,
            @RequestParam String currency) {
        boolean supported = paymentService.isPaymentMethodSupported(paymentMode, currency);
        return ResponseEntity.ok(Map.of("supported", supported));
    }

    @GetMapping("/payment-methods/fee")
    @Operation(summary = "Calculate processing fee for payment method")
    public ResponseEntity<Map<String, Object>> calculateProcessingFee(
            @RequestParam UserSubscription.PaymentMode paymentMode,
            @RequestParam double amount,
            @RequestParam String currency) {
        double fee = paymentService.calculateProcessingFee(paymentMode, amount, currency);
        double total = amount + fee;

        return ResponseEntity.ok(Map.of(
                "amount", amount,
                "processing_fee", fee,
                "total", total,
                "currency", currency
        ));
    }

    @GetMapping("/payment-instructions")
    @Operation(summary = "Get payment instructions for a payment method")
    public ResponseEntity<Map<String, Object>> getPaymentInstructions(
            @RequestParam UserSubscription.PaymentMode paymentMode,
            @RequestParam double amount,
            @RequestParam String currency) {
        Map<String, Object> instructions = paymentService.generatePaymentInstructions(paymentMode, amount, currency);
        return ResponseEntity.ok(instructions);
    }
}
