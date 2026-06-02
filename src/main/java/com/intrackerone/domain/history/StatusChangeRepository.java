package com.intrackerone.domain.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusChangeRepository extends JpaRepository<StatusChange, Long> {

    List<StatusChange> findByJobApplicationIdOrderByChangedAtDesc(Long jobApplicationId);

    List<StatusChange> findByJobApplicationOwnerIdOrderByChangedAtDesc(Long ownerId);

    List<StatusChange> findByToStatusId(Long statusId);
}
