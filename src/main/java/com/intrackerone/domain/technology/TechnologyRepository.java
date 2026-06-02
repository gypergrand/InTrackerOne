package com.intrackerone.domain.technology;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TechnologyRepository extends JpaRepository<Technology, Long> {

    Optional<Technology> findByNameIgnoreCase(String name);
}
