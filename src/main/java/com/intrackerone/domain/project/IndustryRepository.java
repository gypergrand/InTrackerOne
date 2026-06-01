package com.intrackerone.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndustryRepository extends JpaRepository<Industry, Long> {

    Optional<Industry> findByNameIgnoreCase(String name);

    List<Industry> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<Industry> findByIdIn(Collection<Long> ids);
}
