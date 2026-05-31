package com.intrackerone.domain.recruiter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {

    List<Recruiter> findByCompanyId(Long companyId);

    List<Recruiter> findByOwnerId(Long ownerId);

    List<Recruiter> findByCompanyIdAndOwnerId(Long companyId, Long ownerId);
}
