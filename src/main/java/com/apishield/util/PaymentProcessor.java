package com.apishield.util;

import com.apishield.model.UserSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessor {

    private final Random random = new Random();

    /**
     * Process payment through various payment gateways (mocked)
     */
    public PaymentResult processPayment(PaymentRequest request) {
        log.info("Processing payment via {} for amount: {} {}",
                request.getPaymentMode(), request.getAmount(), request.getCurrency());

        // Simulate processing time
        try {
            Thread.sleep(1000 + random.nextInt(2000)); // 1-3 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Mock payment success/failure (90% success rate)
        boolean success = random.nextDouble() < 0.9;

        if (success) {
            return processSuccessfulPayment(request);
        } else {
            return processFailedPayment(request);
        }
    }

    private PaymentResult processSuccessfulPayment(PaymentRequest request) {
        String transactionId = generateTransactionId(request.getPaymentMode());

        Map<String, Object> paymentDetails = new HashMap<>();
        paymentDetails.put("gateway", request.getPaymentMode().name());
        paymentDetails.put("transactionId", transactionId);
        paymentDetails.put("amount", request.getAmount());
        paymentDetails.put("currency", request.getCurrency());
        paymentDetails.put("timestamp", System.currentTimeMillis());

        // Add gateway-specific details
        switch (request.getPaymentMode()) {
            case PAYPAL -> {
                paymentDetails.put("paypalOrderId", "ORDER-" + UUID.randomUUID().toString().substring(0, 8));
                paymentDetails.put("payerEmail", "customer@example.com");
            }
            case RAZORPAY -> {
                paymentDetails.put("razorpayOrderId", "order_" + generateRandomString(14));
                paymentDetails.put("razorpaySignature", generateRandomString(64));
            }
            case UPI -> {
                paymentDetails.put("upiId", "customer@paytm");
                paymentDetails.put("vpa", "customer@phonepe");
            }
        }

        log.info("Payment successful: {} - Transaction ID: {}", request.getPaymentMode(), transactionId);

        return PaymentResult.builder()
                .success(true)
                .transactionId(transactionId)
                .message("Payment processed successfully")
                .paymentDetails(paymentDetails)
                .build();
    }

    private PaymentResult processFailedPayment(PaymentRequest request) {
        String[] failureReasons = {
                "Insufficient funds",
                "Card declined",
                "Payment gateway timeout",
                "Invalid payment details",
                "Daily limit exceeded",
                "Network error"
        };

        String reason = failureReasons[random.nextInt(failureReasons.length)];

        log.warn("Payment failed: {} - Reason: {}", request.getPaymentMode(), reason);

        return PaymentResult.builder()
                .success(false)
                .transactionId(null)
                .message("Payment failed: " + reason)
                .errorCode("PAYMENT_FAILED")
                .paymentDetails(Map.of("failureReason", reason))
                .build();
    }

    private String generateTransactionId(UserSubscription.PaymentMode paymentMode) {
        String prefix = switch (paymentMode) {
            case PAYPAL -> "PP";
            case RAZORPAY -> "RZP";
            case UPI -> "UPI";
            case STRIPE -> "STR";
            case BANK_TRANSFER -> "BT";
        };

        return prefix + "_" + System.currentTimeMillis() + "_" + generateRandomString(6);
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Verify payment status (for webhooks and confirmations)
     */
    public boolean verifyPayment(String transactionId, UserSubscription.PaymentMode paymentMode) {
        log.info("Verifying payment: {} via {}", transactionId, paymentMode);

        // Mock verification - in real implementation, call payment gateway API
        return transactionId != null && transactionId.startsWith(getPaymentPrefix(paymentMode));
    }

    private String getPaymentPrefix(UserSubscription.PaymentMode paymentMode) {
        return switch (paymentMode) {
            case PAYPAL -> "PP";
            case RAZORPAY -> "RZP";
            case UPI -> "UPI";
            case STRIPE -> "STR";
            case BANK_TRANSFER -> "BT";
        };
    }

    /**
     * Cancel/Refund payment
     */
    public RefundResult refundPayment(String transactionId, Double amount, UserSubscription.PaymentMode paymentMode) {
        log.info("Processing refund for transaction: {} - Amount: {}", transactionId, amount);

        // Mock refund processing
        boolean success = random.nextDouble() < 0.95; // 95% success rate for refunds

        if (success) {
            String refundId = "REF_" + System.currentTimeMillis();
            return RefundResult.builder()
                    .success(true)
                    .refundId(refundId)
                    .message("Refund processed successfully")
                    .build();
        } else {
            return RefundResult.builder()
                    .success(false)
                    .message("Refund failed - please try again later")
                    .build();
        }
    }

    // Supporting classes
    @lombok.Data
    @lombok.Builder
    public static class PaymentRequest {
        private Double amount;
        private String currency;
        private UserSubscription.PaymentMode paymentMode;
        private String customerEmail;
        private String description;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class PaymentResult {
        private Boolean success;
        private String transactionId;
        private String message;
        private String errorCode;
        private Map<String, Object> paymentDetails;
    }

    @lombok.Data
    @lombok.Builder
    public static class RefundResult {
        private Boolean success;
        private String refundId;
        private String message;
    }
}
