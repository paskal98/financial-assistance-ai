package com.microservice.auth_service.unit.controller;

import com.microservice.auth_service.controller.user.UserController;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User user;
    private UserDetails userDetails;
    private Locale locale;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID(), "test@example.com", "password", false, null, false, null, null);
        userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        locale = Locale.ENGLISH;
    }

    @Test
    void getCurrentUser_success() {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<User> response = userController.getCurrentUser(userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(user, response.getBody());
        verify(userService, times(1)).findByEmail("test@example.com");
    }

    @Test
    void getCurrentUser_notFound() {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.empty());

        ResponseEntity<User> response = userController.getCurrentUser(userDetails);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService, times(1)).findByEmail("test@example.com");
    }

    @Test
    void enable2FA_success() {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Map<String, String> serviceResponse = Map.of("message", "2FA enabled", "secret", "secret", "qrCode", "qrCode");
        when(userService.enable2FA(user, locale)).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = userController.enable2FA(userDetails, locale);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(userService, times(1)).enable2FA(user, locale);
    }

    @Test
    void disable2FA_success() {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Map<String, String> serviceResponse = Map.of("message", "2FA disabled");
        when(userService.disable2FA(user, locale)).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = userController.disable2FA(userDetails, locale);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(userService, times(1)).disable2FA(user, locale);
    }
}