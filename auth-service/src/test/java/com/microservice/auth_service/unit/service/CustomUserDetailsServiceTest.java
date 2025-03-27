package com.microservice.auth_service.unit.service;

import com.microservice.auth_service.model.entity.Role;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.service.user.CustomUserDetailsService;
import com.microservice.auth_service.service.util.LocalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID(), "test@example.com", "password", false, Set.of(new Role(1, "USER")), false, null, null);
    }

    @Test
    void loadUserByUsername_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("test@example.com");

        assertEquals("test@example.com", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("USER")));
    }

    @Test
    void loadUserByUsername_notFound_throwsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(localizationService.getMessage("error.user.not_found", Locale.getDefault(), "test@example.com")).thenReturn("User not found");

        assertThrows(UsernameNotFoundException.class, () -> customUserDetailsService.loadUserByUsername("test@example.com"));
    }
}