package com.intrackerone.infrastructure.security;

import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCurrentUser_whenAuthenticatedUserExistsAndFoundInRepository_returnsUser() {
        // Given
        String username = "testUser";
        User expectedUser = new User();
        expectedUser.setUsername(username);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(expectedUser));

        // When
        User currentUser = securityService.getCurrentUser();

        // Then
        assertNotNull(currentUser);
        assertEquals(expectedUser, currentUser);
        verify(securityContext, times(1)).getAuthentication();
        verify(authentication, times(1)).isAuthenticated();
        verify(authentication, times(1)).getName();
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    void getCurrentUser_whenNoAuthenticatedUserExists_throwsSecurityException() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, () -> securityService.getCurrentUser());
        assertEquals("No authenticated user", exception.getMessage());
        verify(securityContext, times(1)).getAuthentication();
        verify(authentication, never()).isAuthenticated();
        verify(authentication, never()).getName();
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void getCurrentUser_whenAuthenticatedUserExistsButNotFoundInRepository_throwsResourceNotFoundException() {
        // Given
        String username = "testUser";

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> securityService.getCurrentUser());
        assertEquals("User not found with id: 0", exception.getMessage());
        verify(securityContext, times(1)).getAuthentication();
        verify(authentication, times(1)).isAuthenticated();
        verify(authentication, times(1)).getName();
        verify(userRepository, times(1)).findByUsername(username);
    }
}