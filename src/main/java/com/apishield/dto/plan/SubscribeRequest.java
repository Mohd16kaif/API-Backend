package com.apishield.dto.plan;

import com.apishield.model.UserSubscription;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscribeRequest {

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotNull(message = "Payment mode is required")
    private UserSubscription.PaymentMode paymentMode;

    private Boolean autoRenew = false;

    // Payment gateway specific fields (for real implementations)
    private String paymentToken; // For frontend payment processing
    private String paymentMethodId; // Stripe payment method ID
    private String paymentNonce; // PayPal nonce
    private String razorpayPaymentId; // Razorpay payment ID
    private String upiTransactionId; // UPI transaction reference
}
