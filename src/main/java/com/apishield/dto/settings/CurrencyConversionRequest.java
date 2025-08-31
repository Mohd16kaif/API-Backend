package com.apishield.dto.settings;

import com.apishield.model.User;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CurrencyConversionRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    private Double amount;

    @NotNull(message = "From currency is required")
    private User.Currency fromCurrency;

    @NotNull(message = "To currency is required")
    private User.Currency toCurrency;
}
