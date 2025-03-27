package com.microservice.auth_service.unit.controller;

import com.microservice.auth_service.controller.auth.AuthController;
import com.microservice.auth_service.model.dto.auth.AuthRequest;
import com.microservice.auth_service.model.dto.auth.RefreshTokenRequest;
import com.microservice.auth_service.service.authentication.AuthService;
import com.microservice.auth_service.service.util.LocalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private AuthController authController;

    private Locale locale;

    @BeforeEach
    void setUp() {
        locale = Locale.ENGLISH;
    }

    @Test
    void register_success() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");
        Map<String, String> serviceResponse = Map.of("token", "jwt", "refreshToken", "refresh", "message", "registered");
        when(authService.register("test@example.com", "password", locale)).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.register(request, locale);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(authService, times(1)).register("test@example.com", "password", locale);
    }

    @Test
    void login_success() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");
        Map<String, String> serviceResponse = Map.of("token", "jwt", "refreshToken", "refresh", "message", "logged in");
        when(authService.login(request, locale)).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.login(request, locale);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(authService, times(1)).login(request, locale);
    }

    @Test
    void refreshToken_success() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshTokenUUID("refresh-token-id");
        Map<String, String> serviceResponse = Map.of("token", "new-jwt", "message", "refreshed");
        when(authService.refreshToken("refresh-token-id", locale)).thenReturn(serviceResponse);

        ResponseEntity<Map<String, String>> response = authController.refreshToken(request, locale);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(serviceResponse, response.getBody());
        verify(authService, times(1)).refreshToken("refresh-token-id", locale);
    }

    @Test
    void logout_success() {
        Map<String, String> request = Map.of("email", "test@example.com");
        when(localizationService.getMessage("success.logout", locale)).thenReturn("Logged out");

        ResponseEntity<Map<String, String>> response = authController.logout(request, locale);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Map.of("message", "Logged out"), response.getBody());
        verify(authService, times(1)).logout("test@example.com", locale);
    }
}