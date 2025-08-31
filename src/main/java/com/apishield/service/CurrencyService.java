package com.apishield.service;

import com.apishield.dto.settings.CurrencyConversionRequest;
import com.apishield.dto.settings.CurrencyConversionResponse;
import com.apishield.model.CurrencyRate;
import com.apishield.model.User;
import com.apishield.repository.CurrencyRateRepository;
import com.apishield.util.CurrencyConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final CurrencyRateRepository currencyRateRepository;
    private final CurrencyConverter currencyConverter;

    // Add this method for the refresh endpoint
    @Transactional
    public void refreshExchangeRates() {
        log.info("Refreshing exchange rates from external API");

        try {
            // Clean up any corrupted records first
            cleanupCorruptedRecords();

            // Fetch latest rates
            Map<String, Double> latestRates = fetchLatestRatesFromAPI();

            for (Map.Entry<String, Double> entry : latestRates.entrySet()) {
                String[] currencies = entry.getKey().split("_TO_");
                if (currencies.length == 2) {
                    try {
                        User.Currency fromCurrency = User.Currency.valueOf(currencies[0]);
                        User.Currency toCurrency = User.Currency.valueOf(currencies[1]);
                        Double newRate = entry.getValue();

                        updateOrCreateRate(fromCurrency, toCurrency, newRate);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid currency pair: {}", entry.getKey());
                    }
                }
            }

            log.info("Successfully refreshed {} exchange rates", latestRates.size());
        } catch (Exception e) {
            log.error("Error during currency rates refresh: ", e);
            throw new RuntimeException("Failed to update exchange rates: " + e.getMessage());
        }
    }

    private void cleanupCorruptedRecords() {
        try {
            List<CurrencyRate> allRates = currencyRateRepository.findAll();
            for (CurrencyRate rate : allRates) {
                if (rate.getIsActive() == null) {
                    log.info("Fixing corrupted record: {} -> {}", rate.getFromCurrency(), rate.getToCurrency());
                    rate.setIsActive(true);
                    if (rate.getCreatedAt() == null) {
                        rate.setCreatedAt(LocalDateTime.now());
                    }
                    if (rate.getUpdatedAt() == null) {
                        rate.setUpdatedAt(LocalDateTime.now());
                    }
                    currencyRateRepository.save(rate);
                }
            }
        } catch (Exception e) {
            log.warn("Error cleaning up corrupted records: {}", e.getMessage());
        }
    }

    private void updateOrCreateRate(User.Currency fromCurrency, User.Currency toCurrency, Double newRate) {
        try {
            // Try to find existing rate
            Optional<CurrencyRate> existingRateOpt = currencyRateRepository
                    .findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);

            if (existingRateOpt.isPresent()) {
                // Update existing rate
                CurrencyRate rate = existingRateOpt.get();
                log.info("Updating existing rate: {} -> {} (ID: {})", fromCurrency, toCurrency, rate.getId());
                rate.setRate(newRate);
                rate.setUpdatedAt(LocalDateTime.now());
                rate.setSource("EXTERNAL_API");
                rate.setIsActive(true);
                currencyRateRepository.save(rate);
            } else {
                // Try fallback methods
                Optional<CurrencyRate> fallbackRate = currencyRateRepository
                        .findByFromCurrencyAndToCurrencyAndIsActive(fromCurrency, toCurrency, true);

                if (fallbackRate.isPresent()) {
                    CurrencyRate rate = fallbackRate.get();
                    log.info("Updating via fallback: {} -> {} (ID: {})", fromCurrency, toCurrency, rate.getId());
                    rate.setRate(newRate);
                    rate.setUpdatedAt(LocalDateTime.now());
                    rate.setSource("EXTERNAL_API");
                    currencyRateRepository.save(rate);
                } else {
                    // Create new rate
                    log.info("Creating new rate: {} -> {}", fromCurrency, toCurrency);
                    CurrencyRate newCurrencyRate = CurrencyRate.builder()
                            .fromCurrency(fromCurrency)
                            .toCurrency(toCurrency)
                            .rate(newRate)
                            .source("EXTERNAL_API")
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    currencyRateRepository.save(newCurrencyRate);
                }
            }
        } catch (Exception e) {
            log.error("Error processing rate {} -> {}: {}", fromCurrency, toCurrency, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public CurrencyConversionResponse convertCurrency(CurrencyConversionRequest request) {
        log.info("Converting {} {} to {}", request.getAmount(), request.getFromCurrency(), request.getToCurrency());

        double exchangeRate = currencyConverter.getExchangeRate(request.getFromCurrency(), request.getToCurrency());
        double convertedAmount = request.getAmount() * exchangeRate;

        // Get rate information
        Optional<CurrencyRate> rateInfo = currencyRateRepository
                .findLatestRate(request.getFromCurrency(), request.getToCurrency());

        LocalDateTime rateTimestamp = rateInfo.map(CurrencyRate::getUpdatedAt).orElse(null);
        String rateSource = rateInfo.map(CurrencyRate::getSource).orElse("Fallback");
        boolean isRateRecent = rateInfo.map(CurrencyRate::isRecent).orElse(false);

        String fromSymbol = currencyConverter.getCurrencySymbol(request.getFromCurrency());
        String toSymbol = currencyConverter.getCurrencySymbol(request.getToCurrency());

        String formattedOriginal = currencyConverter.formatAmount(request.getAmount(), request.getFromCurrency());
        String formattedConverted = currencyConverter.formatAmount(convertedAmount, request.getToCurrency());
        String conversionSummary = currencyConverter.getConversionSummary(
                request.getAmount(), request.getFromCurrency(), convertedAmount, request.getToCurrency());

        return CurrencyConversionResponse.builder()
                .originalAmount(request.getAmount())
                .fromCurrency(request.getFromCurrency())
                .fromCurrencySymbol(fromSymbol)
                .convertedAmount(Math.round(convertedAmount * 100.0) / 100.0)
                .toCurrency(request.getToCurrency())
                .toCurrencySymbol(toSymbol)
                .exchangeRate(Math.round(exchangeRate * 10000.0) / 10000.0)
                .formattedOriginal(formattedOriginal)
                .formattedConverted(formattedConverted)
                .rateTimestamp(rateTimestamp)
                .rateSource(rateSource)
                .isRateRecent(isRateRecent)
                .conversionSummary(conversionSummary)
                .inverseRate(Math.round((1.0 / exchangeRate) * 10000.0) / 10000.0)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllExchangeRates() {
        log.info("Fetching all exchange rates");

        Map<String, Object> response = new HashMap<>();
        Map<String, Double> rates = currencyConverter.getAllExchangeRates();

        response.put("rates", rates);
        response.put("base_currency", "USD");
        response.put("timestamp", LocalDateTime.now());
        response.put("source", "API Spend Shield");

        // Add formatted rates for display
        Map<String, String> formattedRates = new HashMap<>();
        rates.forEach((pair, rate) -> {
            String[] currencies = pair.split("_TO_");
            if (currencies.length == 2) {
                formattedRates.put(pair, String.format("1 %s = %.4f %s", currencies[0], rate, currencies[1]));
            }
        });
        response.put("formatted_rates", formattedRates);

        return response;
    }

    @Transactional
    public void updateExchangeRates() {
        log.info("Updating exchange rates from external API");

        // Mock external API call - in production, call real exchange rate API
        Map<String, Double> latestRates = fetchLatestRatesFromAPI();

        latestRates.forEach((pair, rate) -> {
            String[] currencies = pair.split("_TO_");
            if (currencies.length == 2) {
                try {
                    User.Currency fromCurrency = User.Currency.valueOf(currencies[0]);
                    User.Currency toCurrency = User.Currency.valueOf(currencies[1]);

                    currencyConverter.updateExchangeRate(fromCurrency, toCurrency, rate, "EXTERNAL_API");
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid currency pair: {}", pair);
                }
            }
        });

        log.info("Successfully updated {} exchange rates", latestRates.size());
    }

    @Transactional(readOnly = true)
    public List<CurrencyRate> getStaleRates() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        return currencyRateRepository.findStaleRates(cutoff);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCurrencyStats() {
        Map<String, Object> stats = new HashMap<>();

        List<CurrencyRate> allRates = currencyRateRepository.findAllActiveRates();
        stats.put("total_rates", allRates.size());

        long recentRates = allRates.stream()
                .mapToLong(rate -> rate.isRecent() ? 1 : 0)
                .sum();
        stats.put("recent_rates", recentRates);
        stats.put("stale_rates", allRates.size() - recentRates);

        // Get rate sources
        Map<String, Long> sourceCounts = allRates.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CurrencyRate::getSource,
                        java.util.stream.Collectors.counting()
                ));
        stats.put("sources", sourceCounts);

        return stats;
    }

    /**
     * Mock external API call for exchange rates
     * In production, this would call a real exchange rate API like:
     * - ExchangeRate-API
     * - CurrencyAPI
     * - Open Exchange Rates
     * - etc.
     */
    private Map<String, Double> fetchLatestRatesFromAPI() {
        // Mock rates with slight variations to simulate real API
        Map<String, Double> rates = new HashMap<>();

        // Base rates with small random variations (±2%)
        double usdToInrBase = 83.0;
        double variation = (Math.random() - 0.5) * 0.04; // ±2%
        double currentUsdToInr = usdToInrBase * (1 + variation);

        rates.put("USD_TO_INR", Math.round(currentUsdToInr * 10000.0) / 10000.0);
        rates.put("INR_TO_USD", Math.round((1.0 / currentUsdToInr) * 10000.0) / 10000.0);

        log.debug("Fetched mock rates: USD to INR = {}, INR to USD = {}",
                rates.get("USD_TO_INR"), rates.get("INR_TO_USD"));

        return rates;
    }

    /**
     * Convert all financial data for a user to their preferred currency
     */
    @Transactional(readOnly = true)
    public Map<String, Double> convertUserFinancialData(User user, Map<String, Double> amounts,
                                                        User.Currency fromCurrency) {
        return currencyConverter.batchConvert(amounts, fromCurrency, user.getCurrencyPreference());
    }
}