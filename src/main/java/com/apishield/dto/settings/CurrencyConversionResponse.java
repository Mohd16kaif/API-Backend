package com.apishield.dto.settings;

import com.apishield.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyConversionResponse {
    private Double originalAmount;
    private User.Currency fromCurrency;
    private String fromCurrencySymbol;
    private Double convertedAmount;
    private User.Currency toCurrency;
    private String toCurrencySymbol;
    private Double exchangeRate;
    private String formattedOriginal;
    private String formattedConverted;
    private LocalDateTime rateTimestamp;
    private String rateSource;
    private Boolean isRateRecent;

    // Additional context
    private String conversionSummary; // "100.00 USD = 8,300.00 INR"
    private Double inverseRate; // Rate for reverse conversion
}
