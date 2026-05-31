package com.intrackerone.domain.user;

import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiTokenService {

    private static final int TOKEN_BYTES = 32;

    private final ApiTokenRepository apiTokenRepository;
    private final ApiTokenMapper apiTokenMapper;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ApiTokenDto> findByUserId(Long userId) {
        return apiTokenRepository.findByUserId(userId).stream()
                .map(apiTokenMapper::toDto)
                .toList();
    }

    @Transactional
    public CreatedToken create(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        byte[] tokenBytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(tokenBytes);
        String rawToken = "itr_" + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        String tokenHash = hashToken(rawToken);

        ApiToken token = ApiToken.builder()
                .name(name)
                .tokenHash(tokenHash)
                .tokenIdentifier(rawToken.substring(4, 20))
                .user(user)
                .enabled(true)
                .build();
        token = apiTokenRepository.save(token);

        log.info("Created API token '{}' for user {}", name, userId);
        return new CreatedToken(token.getId(), token.getName(), rawToken, token.getCreatedAt());
    }

    @Transactional
    public void revoke(Long tokenId, Long userId) {
        ApiToken token = apiTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("ApiToken", tokenId));
        if (!token.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Token does not belong to user");
        }
        token.setEnabled(false);
        apiTokenRepository.save(token);
        log.info("Revoked API token {} for user {}", tokenId, userId);
    }

    public String hashToken(String rawToken) {
        return org.springframework.security.crypto.bcrypt.BCrypt.hashpw(rawToken, org.springframework.security.crypto.bcrypt.BCrypt.gensalt());
    }

    public boolean verifyToken(String rawToken, String tokenHash) {
        return org.springframework.security.crypto.bcrypt.BCrypt.checkpw(rawToken, tokenHash);
    }

    public record CreatedToken(Long id, String name, String token, java.time.LocalDateTime createdAt) {}
}
