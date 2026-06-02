package com.intrackerone.domain.question;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionForm {
    @NotBlank(message = "Question is required")
    private String question;
    @NotBlank(message = "Answer is required")
    private String answer;
    private String notes;
    private Long recruiterId;
}
