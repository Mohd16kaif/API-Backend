package com.apishield.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "currency_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"from_currency", "to_currency"}))
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_currency", nullable = false)
    private User.Currency fromCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_currency", nullable = false)
    private User.Currency toCurrency;

    @Column(nullable = false)
    private Double rate;

    @Column(name = "source")
    private String source = "API"; // Source of exchange rate (API, MANUAL, etc.)

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    @Transient
    public boolean isRecent() {
        if (updatedAt == null) return false;
        return updatedAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    @Transient
    public String getPairString() {
        return fromCurrency.name() + "/" + toCurrency.name();
    }

    @Transient
    public double getInverseRate() {
        return rate != 0 ? 1.0 / rate : 0.0;
    }

    public void updateRate(double newRate, String source) {
        this.rate = newRate;
        this.source = source;
        this.updatedAt = LocalDateTime.now();
    }
}
