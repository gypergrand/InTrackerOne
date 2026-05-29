package com.intrackerone.domain.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findByOwnerId(Long ownerId);

    List<JobApplication> findByProjectId(Long projectId);

    List<JobApplication> findByCurrentStatusId(Long statusId);

    List<JobApplication> findByOwnerIdAndCurrentStatusId(Long ownerId, Long statusId);

    long countByOwnerId(Long ownerId);

    long countByOwnerIdAndCurrentStatusId(Long ownerId, Long statusId);
}
