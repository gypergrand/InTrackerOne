package com.intrackerone.domain.question;

public record QuestionDto(
    Long id,
    String question,
    String answer,
    String notes,
    Long recruiterId,
    String recruiterName,
    Long ownerId,
    String ownerName,
    boolean isDefault
) {}
