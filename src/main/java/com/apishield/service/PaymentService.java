package com.apishield.service;

import com.apishield.model.UserSubscription;
import com.apishield.util.PaymentProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentProcessor paymentProcessor;

    /**
     * Get payment methods available for a payment mode
     */
    public Map<String, Object> getPaymentMethods(UserSubscription.PaymentMode paymentMode) {
        return switch (paymentMode) {
            case PAYPAL -> Map.of(
                    "name", "PayPal",
                    "description", "Pay securely with your PayPal account",
                    "supported_currencies", new String[]{"USD", "INR"},
                    "processing_time", "Instant",
                    "fees", "2.9% + $0.30"
            );
            case RAZORPAY -> Map.of(
                    "name", "Razorpay",
                    "description", "UPI, Cards, Net Banking, and Wallets",
                    "supported_currencies", new String[]{"INR"},
                    "processing_time", "Instant",
                    "fees", "2% + ₹2"
            );
            case UPI -> Map.of(
                    "name", "UPI",
                    "description", "Pay using any UPI app",
                    "supported_currencies", new String[]{"INR"},
                    "processing_time", "Instant",
                    "fees", "Free"
            );
            case STRIPE -> Map.of(
                    "name", "Stripe",
                    "description", "Credit/Debit Cards",
                    "supported_currencies", new String[]{"USD", "INR"},
                    "processing_time", "Instant",
                    "fees", "2.9% + $0.30"
            );
            case BANK_TRANSFER -> Map.of(
                    "name", "Bank Transfer",
                    "description", "Direct bank transfer",
                    "supported_currencies", new String[]{"USD", "INR"},
                    "processing_time", "1-3 business days",
                    "fees", "Free"
            );
        };
    }

    /**
     * Validate payment method for currency
     */
    public boolean isPaymentMethodSupported(UserSubscription.PaymentMode paymentMode, String currency) {
        Map<String, Object> methods = getPaymentMethods(paymentMode);
        String[] supportedCurrencies = (String[]) methods.get("supported_currencies");

        for (String supportedCurrency : supportedCurrencies) {
            if (supportedCurrency.equals(currency)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get estimated processing fee
     */
    public double calculateProcessingFee(UserSubscription.PaymentMode paymentMode, double amount, String currency) {
        return switch (paymentMode) {
            case PAYPAL -> amount * 0.029 + (currency.equals("USD") ? 0.30 : 25.0);
            case RAZORPAY -> amount * 0.02 + 2.0;
            case UPI -> 0.0; // Free
            case STRIPE -> amount * 0.029 + (currency.equals("USD") ? 0.30 : 25.0);
            case BANK_TRANSFER -> 0.0; // Free
        };
    }

    /**
     * Generate payment instructions
     */
    public Map<String, Object> generatePaymentInstructions(UserSubscription.PaymentMode paymentMode,
                                                           double amount, String currency) {
        return switch (paymentMode) {
            case UPI -> Map.of(
                    "instructions", new String[]{
                            "Open any UPI app (PhonePe, Paytm, Google Pay, etc.)",
                            "Scan the QR code or use UPI ID: apishield@payu",
                            "Enter amount: " + currency + " " + amount,
                            "Complete the payment",
                            "Share transaction ID with us"
                    },
                    "upi_id", "apishield@payu",
                    "qr_code_url", "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=upi://pay?pa=apishield@payu&am=" + amount
            );
            case BANK_TRANSFER -> Map.of(
                    "instructions", new String[]{
                            "Transfer to our bank account",
                            "Use your email as reference",
                            "Send payment proof to billing@apishield.com"
                    },
                    "bank_details", Map.of(
                            "account_name", "API Spend Shield",
                            "account_number", "1234567890",
                            "ifsc", "HDFC0001234",
                            "bank_name", "HDFC Bank"
                    )
            );
            default -> Map.of(
                    "instructions", new String[]{
                            "You will be redirected to " + paymentMode.name() + " payment page",
                            "Complete the payment process",
                            "You will be redirected back to our app"
                    }
            );
        };
    }
}
