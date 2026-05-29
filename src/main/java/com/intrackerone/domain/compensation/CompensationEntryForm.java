package com.intrackerone.domain.compensation;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompensationEntryForm {
    private RateType rateType;
    private BigDecimal amount;
    private Currency currency;
    private CompCategory category;
    private String notes;
}
