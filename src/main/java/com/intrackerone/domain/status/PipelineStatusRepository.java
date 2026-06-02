package com.intrackerone.domain.status;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PipelineStatusRepository extends JpaRepository<PipelineStatus, Long> {

    List<PipelineStatus> findByOwnerIdOrderBySortOrderAsc(Long ownerId);

    List<PipelineStatus> findByIsDefaultTrueOrderBySortOrderAsc();

    boolean existsByOwnerIdAndName(Long ownerId, String name);
}
