package com.intrackerone.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    List<ApiToken> findByUserId(Long userId);

    @Query("SELECT t FROM ApiToken t JOIN FETCH t.user WHERE t.tokenIdentifier = :identifier")
    Optional<ApiToken> findByTokenIdentifier(@Param("identifier") String identifier);
}
