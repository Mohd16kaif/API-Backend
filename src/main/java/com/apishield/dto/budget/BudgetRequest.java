package com.apishield.dto.budget;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BudgetRequest {

    @NotNull(message = "Monthly budget is required")
    @DecimalMin(value = "1.0", message = "Monthly budget must be at least $1.00")
    @DecimalMax(value = "999999.99", message = "Monthly budget must not exceed $999,999.99")
    private Double monthlyBudget;
}
