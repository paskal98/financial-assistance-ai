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

    @Test
    void getCurrentUser_withInvalidToken_fails() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid-jwt-token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String getUserUrl = "http://localhost:" + port + "/user/me";
        ResponseEntity<Map> response = restTemplate.exchange(getUserUrl, HttpMethod.GET, entity, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().get("error").toString().contains("Invalid or expired JWT token"));
    }

    @Test
    void enable2FA_withAlreadyEnabled_fails() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        String token = (String) registerResponse.getBody().get("token");

        headers.setBearerAuth(token);
        HttpEntity<Void> enable2FAEntity = new HttpEntity<>(headers);
        String enable2FAUrl = "http://localhost:" + port + "/user/2fa/enable";
        ResponseEntity<Map> firstResponse = restTemplate.exchange(enable2FAUrl, HttpMethod.POST, enable2FAEntity, Map.class);
        assertEquals(HttpStatus.OK, firstResponse.getStatusCode());

        ResponseEntity<Map> secondResponse = restTemplate.exchange(enable2FAUrl, HttpMethod.POST, enable2FAEntity, Map.class);
        assertEquals(HttpStatus.OK, secondResponse.getStatusCode());
    }

    @Test
    void disable2FAWithBackupCode_withInvalidCode_fails() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        String token = (String) registerResponse.getBody().get("token");

        headers.setBearerAuth(token);
        HttpEntity<Void> enable2FAEntity = new HttpEntity<>(headers);
        String enable2FAUrl = "http://localhost:" + port + "/user/2fa/enable";
        restTemplate.exchange(enable2FAUrl, HttpMethod.POST, enable2FAEntity, Map.class);

        String backupCodesUrl = "http://localhost:" + port + "/user/2fa/backup-codes";
        ResponseEntity<Map> backupResponse = restTemplate.exchange(backupCodesUrl, HttpMethod.POST, enable2FAEntity, Map.class);
        assertEquals(HttpStatus.OK, backupResponse.getStatusCode());

        Map<String, String> request = Map.of("backupCode", "invalid-code");
        HttpEntity<Map<String, String>> disableEntity = new HttpEntity<>(request, headers);
        String disableUrl = "http://localhost:" + port + "/user/2fa/disable-with-backup";
        ResponseEntity<Map> response = restTemplate.exchange(disableUrl, HttpMethod.POST, disableEntity, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid backup code.", response.getBody().get("error"));
    }
}