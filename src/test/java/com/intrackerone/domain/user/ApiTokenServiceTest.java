package com.intrackerone.domain.user;

import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiTokenServiceTest {

    @Mock
    private ApiTokenRepository apiTokenRepository;

    @Mock
    private ApiTokenMapper apiTokenMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApiTokenService apiTokenService;

    @Captor
    private ArgumentCaptor<ApiToken> tokenCaptor;

    private User user;
    private ApiToken token;
    private ApiTokenDto tokenDto;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();

        token = ApiToken.builder()
            .id(1L).name("My Token").enabled(true)
            .tokenHash("$2a$10$hash")
            .user(user).createdAt(LocalDateTime.now())
            .build();

        tokenDto = new ApiTokenDto(1L, "My Token", true, null, LocalDateTime.now());
    }

    @Test
    void findByUserId_returnsTokens() {
        when(apiTokenRepository.findByUserId(1L)).thenReturn(List.of(token));
        when(apiTokenMapper.toDto(token)).thenReturn(tokenDto);

        List<ApiTokenDto> result = apiTokenService.findByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("My Token");
    }

    @Test
    void create_generatesTokenAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(apiTokenRepository.save(any())).thenReturn(token);

        ApiTokenService.CreatedToken result = apiTokenService.create(1L, "My Token");

        assertThat(result.name()).isEqualTo("My Token");
        assertThat(result.token()).startsWith("itr_");
        verify(apiTokenRepository).save(tokenCaptor.capture());
        ApiToken saved = tokenCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("My Token");
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getTokenHash()).isNotEmpty();
    }

    @Test
    void revoke_disablesToken() {
        when(apiTokenRepository.findById(1L)).thenReturn(Optional.of(token));

        apiTokenService.revoke(1L, 1L);

        verify(apiTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getEnabled()).isFalse();
    }

    @Test
    void revoke_whenNotOwner_throwsException() {
        token.setUser(User.builder().id(2L).build());
        when(apiTokenRepository.findById(1L)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> apiTokenService.revoke(1L, 99L))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void revoke_whenNotFound_throwsException() {
        when(apiTokenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiTokenService.revoke(99L, 1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifyToken_checksAgainstHash() {
        String raw = "itr_test123";
        String hash = apiTokenService.hashToken(raw);

        assertThat(apiTokenService.verifyToken(raw, hash)).isTrue();
        assertThat(apiTokenService.verifyToken("wrong", hash)).isFalse();
    }
}
