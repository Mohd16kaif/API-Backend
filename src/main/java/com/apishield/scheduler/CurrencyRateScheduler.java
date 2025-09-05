package com.apishield.scheduler;

import com.apishield.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrencyRateScheduler {

    private final CurrencyService currencyService;

    /**
     * Update exchange rates every 6 hours
     */
    @Scheduled(fixedRate = 21600000) // 6 hours in milliseconds
    public void updateExchangeRates() {
        log.info("Starting scheduled exchange rate update...");

        try {
            currencyService.updateExchangeRates();
            log.info("Successfully completed scheduled exchange rate update");
        } catch (DataIntegrityViolationException e) {
            // Handle duplicate key errors gracefully - don't crash the app
            log.warn("Duplicate currency rate detected - skipping this update cycle: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error during scheduled exchange rate update", e);
            // Don't rethrow - let the scheduler continue running
        }
    }

    /**
     * Clean up old exchange rates - runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldRates() {
        log.info("Starting cleanup of old exchange rates...");

        try {
            var staleRates = currencyService.getStaleRates();
            log.info("Found {} stale exchange rates", staleRates.size());

            if (staleRates.size() > 10) {
                log.warn("High number of stale rates detected - may need to check external API");
            }

        } catch (Exception e) {
            log.error("Error during exchange rate cleanup", e);
            // Don't rethrow - let the scheduler continue
        }
    }
}