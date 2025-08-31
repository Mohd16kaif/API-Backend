package com.apishield.health;

import com.apishield.repository.CurrencyRateRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CurrencyRateHealthIndicator implements HealthIndicator {

    private final CurrencyRateRepository currencyRateRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CurrencyRateHealthIndicator(CurrencyRateRepository currencyRateRepository) {
        this.currencyRateRepository = currencyRateRepository;
    }

    @Override
    public Health health() {
        try {
            // Use basic count method that should exist
            long totalRates = currencyRateRepository.count();

            if (totalRates == 0) {
                return Health.status("DEGRADED")
                        .withDetail("status", "No currency rates configured")
                        .withDetail("message", "Currency rate data is not available - system may still function with default rates")
                        .withDetail("total_rates", 0)
                        .withDetail("check_time", LocalDateTime.now().format(FORMATTER))
                        .build();
            }

            return Health.up()
                    .withDetail("status", "Currency rates available")
                    .withDetail("message", "Currency rate system is operational")
                    .withDetail("total_rates", totalRates)
                    .withDetail("check_time", LocalDateTime.now().format(FORMATTER))
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "Currency rate check failed")
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("error_message", e.getMessage() != null ? e.getMessage() : "Unknown error occurred")
                    .withDetail("check_time", LocalDateTime.now().format(FORMATTER))
                    .build();
        }
    }
}
