package com.intrackerone.domain.recruiter;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class RecruiterForm {
    private String name;
    private String email;
    private String phone;
    private String notes;
    private Long companyId;
    private Set<Long> projectIds = new HashSet<>();
}
