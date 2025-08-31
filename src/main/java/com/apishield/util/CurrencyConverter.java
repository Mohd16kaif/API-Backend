package com.apishield.util;

import com.apishield.model.CurrencyRate;
import com.apishield.model.User;
import com.apishield.repository.CurrencyRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrencyConverter {

    private final CurrencyRateRepository currencyRateRepository;

    // Fallback rates if database rates are not available
    private static final Map<String, Double> FALLBACK_RATES = Map.of(
            "USD_TO_INR", 83.0,
            "INR_TO_USD", 0.012048
    );

    /**
     * Convert amount from one currency to another
     */
    public double convertCurrency(double amount, User.Currency fromCurrency, User.Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return amount;
        }

        double rate = getExchangeRate(fromCurrency, toCurrency);
        return amount * rate;
    }

    /**
     * Get current exchange rate between two currencies
     */
    public double getExchangeRate(User.Currency fromCurrency, User.Currency toCurrency) {
        if (fromCurrency == toCurrency) {
            return 1.0;
        }

        // Try to get rate from database first
        Optional<CurrencyRate> currencyRate = currencyRateRepository
                .findLatestRate(fromCurrency, toCurrency);

        if (currencyRate.isPresent() && currencyRate.get().isRecent()) {
            log.debug("Using database rate: {} {} to {}", currencyRate.get().getRate(), fromCurrency, toCurrency);
            return currencyRate.get().getRate();
        }

        // Fallback to hardcoded rates
        String rateKey = fromCurrency.name() + "_TO_" + toCurrency.name();
        Double fallbackRate = FALLBACK_RATES.get(rateKey);

        if (fallbackRate != null) {
            log.debug("Using fallback rate: {} {} to {}", fallbackRate, fromCurrency, toCurrency);
            return fallbackRate;
        }

        log.warn("No exchange rate found for {} to {}, using rate 1.0", fromCurrency, toCurrency);
        return 1.0;
    }

    /**
     * Get currency symbol
     */
    public String getCurrencySymbol(User.Currency currency) {
        return switch (currency) {
            case USD -> "$";
            case INR -> "₹";
        };
    }

    /**
     * Format amount with currency symbol
     */
    public String formatAmount(double amount, User.Currency currency) {
        String symbol = getCurrencySymbol(currency);
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return symbol + formatter.format(amount);
    }

    /**
     * Format amount with currency code
     */
    public String formatAmountWithCode(double amount, User.Currency currency) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        return formatter.format(amount) + " " + currency.name();
    }

    /**
     * Get conversion summary string
     */
    public String getConversionSummary(double originalAmount, User.Currency fromCurrency,
                                       double convertedAmount, User.Currency toCurrency) {
        return String.format("%s = %s",
                formatAmountWithCode(originalAmount, fromCurrency),
                formatAmountWithCode(convertedAmount, toCurrency));
    }

    /**
     * Update exchange rate in database
     */
    public void updateExchangeRate(User.Currency fromCurrency, User.Currency toCurrency,
                                   double rate, String source) {
        Optional<CurrencyRate> existingRate = currencyRateRepository
                .findByFromCurrencyAndToCurrencyAndIsActive(fromCurrency, toCurrency, true);

        if (existingRate.isPresent()) {
            CurrencyRate currencyRate = existingRate.get();
            currencyRate.updateRate(rate, source);
            currencyRateRepository.save(currencyRate);
            log.info("Updated exchange rate: {} {} to {} from {}", rate, fromCurrency, toCurrency, source);
        } else {
            CurrencyRate newRate = CurrencyRate.builder()
                    .fromCurrency(fromCurrency)
                    .toCurrency(toCurrency)
                    .rate(rate)
                    .source(source)
                    .updatedAt(LocalDateTime.now())
                    .build();
            currencyRateRepository.save(newRate);
            log.info("Created new exchange rate: {} {} to {} from {}", rate, fromCurrency, toCurrency, source);
        }
    }

    /**
     * Get all available currency rates
     */
    public Map<String, Double> getAllExchangeRates() {
        Map<String, Double> rates = new HashMap<>();

        var allRates = currencyRateRepository.findAllActiveRates();
        for (CurrencyRate rate : allRates) {
            String key = rate.getFromCurrency().name() + "_TO_" + rate.getToCurrency().name();
            rates.put(key, rate.getRate());
        }

        // Add fallback rates if not present
        FALLBACK_RATES.forEach(rates::putIfAbsent);

        return rates;
    }

    /**
     * Check if rate is stale (older than 24 hours)
     */
    public boolean isRateStale(User.Currency fromCurrency, User.Currency toCurrency) {
        Optional<CurrencyRate> rate = currencyRateRepository.findLatestRate(fromCurrency, toCurrency);
        return rate.isEmpty() || !rate.get().isRecent();
    }

    /**
     * Get rate source information
     */
    public String getRateSource(User.Currency fromCurrency, User.Currency toCurrency) {
        Optional<CurrencyRate> rate = currencyRateRepository.findLatestRate(fromCurrency, toCurrency);

        if (rate.isPresent()) {
            return rate.get().getSource() + " (Updated: " + rate.get().getUpdatedAt() + ")";
        }

        return "Fallback rate";
    }

    /**
     * Convert user's budget/spending amounts to their preferred currency
     */
    public double convertToUserCurrency(double amount, User.Currency fromCurrency, User user) {
        return convertCurrency(amount, fromCurrency, user.getCurrencyPreference());
    }

    /**
     * Batch convert multiple amounts
     */
    public Map<String, Double> batchConvert(Map<String, Double> amounts,
                                            User.Currency fromCurrency, User.Currency toCurrency) {
        Map<String, Double> converted = new HashMap<>();
        double rate = getExchangeRate(fromCurrency, toCurrency);

        amounts.forEach((key, amount) -> {
            converted.put(key, amount * rate);
        });

        return converted;
    }
}
