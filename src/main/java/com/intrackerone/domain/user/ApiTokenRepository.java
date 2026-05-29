package com.intrackerone.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiTokenRepository extends JpaRepository<ApiToken, Long> {

    List<ApiToken> findByUserId(Long userId);

    Optional<ApiToken> findByTokenHash(String tokenHash);
}
