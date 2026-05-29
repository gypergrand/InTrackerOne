package com.intrackerone.domain.compensation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompensationEntryRepository extends JpaRepository<CompensationEntry, Long> {

    List<CompensationEntry> findByJobApplicationId(Long jobApplicationId);
}
