package com.intrackerone.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByCompanyId(Long companyId);

    List<Project> findByOwnerId(Long ownerId);

    List<Project> findByCompanyIdAndOwnerId(Long companyId, Long ownerId);
}
