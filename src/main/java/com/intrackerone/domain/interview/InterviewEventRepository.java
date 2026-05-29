package com.intrackerone.domain.interview;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewEventRepository extends JpaRepository<InterviewEvent, Long> {

    List<InterviewEvent> findByJobApplicationIdOrderByEventDateAsc(Long jobApplicationId);
}
