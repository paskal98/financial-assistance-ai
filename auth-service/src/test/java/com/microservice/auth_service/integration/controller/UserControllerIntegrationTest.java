package com.microservice.auth_service.integration.controller;

import com.microservice.auth_service.integration.BaseControllerIntegrationTest;
import com.microservice.auth_service.model.dto.auth.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UserControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Test
    void getCurrentUser_integration() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        String token = (String) registerResponse.getBody().get("token");

        headers.setBearerAuth(token);
        HttpEntity<Void> getEntity = new HttpEntity<>(headers);

        String getUserUrl = "http://localhost:" + port + "/user/me";
        ResponseEntity<Map> response = restTemplate.exchange(getUserUrl, HttpMethod.GET, getEntity, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("test@example.com", response.getBody().get("email"));
    }

    @Test
    void enable2FA_integration() {

        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test2@example.com");
        registerRequest.setPassword("password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        String token = (String) registerResponse.getBody().get("token");

        headers.setBearerAuth(token);
        HttpEntity<Void> enable2FAEntity = new HttpEntity<>(headers);

        String enable2FAUrl = "http://localhost:" + port + "/user/2fa/enable";
        ResponseEntity<Map> response = restTemplate.exchange(enable2FAUrl, HttpMethod.POST, enable2FAEntity, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Two-factor authentication has been enabled.", response.getBody().get("message"));
        assertNotNull(response.getBody().get("secret"));
        assertNotNull(response.getBody().get("qrCode"));

        HttpEntity<Void> getEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "http://localhost:" + port + "/user/me", HttpMethod.GET, getEntity, Map.class);
        assertTrue((Boolean) userResponse.getBody().get("2FAEnabled"));
        assertNotNull(userResponse.getBody().get("twoFASecret"));
    }
}