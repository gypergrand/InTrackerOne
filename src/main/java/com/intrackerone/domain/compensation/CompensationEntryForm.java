package com.intrackerone.domain.compensation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompensationEntryForm {
    @NotNull(message = "Rate type is required")
    private RateType rateType;
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    @NotNull(message = "Currency is required")
    private Currency currency;
    @NotNull(message = "Category is required")
    private CompCategory category;
    private String notes;
}
