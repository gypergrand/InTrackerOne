package com.intrackerone.domain.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByOwnerId(Long ownerId);

    List<Company> findAllByOrderByNameAsc();
}
