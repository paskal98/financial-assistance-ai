package com.microservice.auth_service.integration.service;

import com.microservice.auth_service.integration.BaseIntegrationTest;
import com.microservice.auth_service.model.dto.auth.AuthRequest;
import com.microservice.auth_service.model.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceIntegrationTest extends BaseIntegrationTest {

    @Test
    void registerAndLogin_integration() {
        Map<String, String> registerResponse = authService.register("test@example.com", "password123", Locale.ENGLISH);
        assertNotNull(registerResponse.get("token"));
        assertNotNull(registerResponse.get("refreshToken"));
        assertEquals("Registration successful.", registerResponse.get("message"));

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        Map<String, String> loginResponse = authService.login(loginRequest, Locale.ENGLISH);
        assertNotNull(loginResponse.get("token"));
        assertNotNull(loginResponse.get("refreshToken"));
        assertEquals("Login successful.", loginResponse.get("message"));

        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        assertEquals("test@example.com", user.getEmail());
        assertTrue(user.getRoles().stream().anyMatch(role -> role.getName().equals("USER")));
    }

    @Test
    void refreshToken_integration() {
        authService.register("test@example.com", "password123", Locale.ENGLISH);
        AuthRequest refreshTokenRequest = new AuthRequest();
        refreshTokenRequest.setEmail("test@example.com");
        refreshTokenRequest.setPassword("password123");
        String refreshToken = authService.login(refreshTokenRequest, Locale.ENGLISH)
                .get("refreshToken");

        Map<String, String> refreshResponse = authService.refreshToken(refreshToken, Locale.ENGLISH);
        assertNotNull(refreshResponse.get("token"));
        assertEquals("Token refreshed successfully.", refreshResponse.get("message"));
    }
}
