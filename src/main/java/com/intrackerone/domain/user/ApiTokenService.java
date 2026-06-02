package com.intrackerone.domain.user;

import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
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
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public record CreatedToken(Long id, String name, String token, java.time.LocalDateTime createdAt) {}
}
