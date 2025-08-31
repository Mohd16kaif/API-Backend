package com.apishield.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "amount_paid")
    private Double amountPaid;

    @Column(name = "currency_paid")
    @Enumerated(EnumType.STRING)
    private User.Currency currencyPaid;

    @Column(name = "payment_reference")
    private String paymentReference; // Transaction ID from payment gateway

    @Column(name = "payment_details", columnDefinition = "TEXT")
    private String paymentDetails; // JSON string with additional payment info

    @Column(name = "auto_renew")
    private Boolean autoRenew = false;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum PaymentMode {
        PAYPAL,
        RAZORPAY,
        UPI,
        STRIPE,
        BANK_TRANSFER
    }

    public enum Status {
        ACTIVE,
        EXPIRED,
        CANCELLED,
        PENDING,
        FAILED
    }

    // Helper methods
    @Transient
    public boolean isActive() {
        return status == Status.ACTIVE &&
                (endDate == null || endDate.isAfter(LocalDate.now()));
    }

    @Transient
    public boolean isExpired() {
        return status == Status.EXPIRED ||
                (endDate != null && endDate.isBefore(LocalDate.now()));
    }

    @Transient
    public long getDaysRemaining() {
        if (endDate == null) return -1;
        LocalDate today = LocalDate.now();
        return today.isBefore(endDate) ?
                java.time.temporal.ChronoUnit.DAYS.between(today, endDate) : 0;
    }

    @Transient
    public double getUtilizationPercentage() {
        if (startDate == null || endDate == null) return 0.0;

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now());

        if (totalDays <= 0) return 100.0;
        return Math.min(100.0, (elapsedDays / (double) totalDays) * 100.0);
    }

    @Transient
    public String getFormattedAmountPaid() {
        if (amountPaid == null || currencyPaid == null) {
            return "N/A";
        }
        String symbol = currencyPaid == User.Currency.USD ? "$" : "₹";
        return String.format("%s%.2f", symbol, amountPaid);
    }

    public void activate() {
        this.status = Status.ACTIVE;
        if (this.startDate == null) {
            this.startDate = LocalDate.now();
        }
        if (this.endDate == null) {
            this.endDate = this.startDate.plusMonths(1);
        }
    }

    public void cancel(String reason) {
        this.status = Status.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
        this.autoRenew = false;
    }

    public void expire() {
        this.status = Status.EXPIRED;
    }
}
