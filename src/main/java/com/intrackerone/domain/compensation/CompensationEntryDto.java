package com.intrackerone.domain.compensation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompensationEntryDto(
    Long id,
    Long jobApplicationId,
    RateType rateType,
    BigDecimal amount,
    Currency currency,
    CompCategory category,
    String notes,
    LocalDateTime createdAt
) {}
