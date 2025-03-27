package com.microservice.auth_service.integration.controller;

import com.microservice.auth_service.integration.BaseControllerIntegrationTest;
import com.microservice.auth_service.model.dto.auth.AuthRequest;
import com.microservice.auth_service.model.dto.auth.RefreshTokenRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AuthControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Test
    void registerAndLogin_integration() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody().get("token"));

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
        HttpEntity<AuthRequest> loginEntity = new HttpEntity<>(loginRequest, headers);

        String loginUrl = "http://localhost:" + port + "/auth/login";
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(loginUrl, loginEntity, Map.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody().get("token"));
    }

    @Test
    void refreshToken_integration() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test2@example.com");
        registerRequest.setPassword("password123");
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, new HttpHeaders());

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);

        String refreshToken = (String) registerResponse.getBody().get("refreshToken");
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshTokenUUID(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RefreshTokenRequest> refreshEntity = new HttpEntity<>(refreshRequest, headers);

        String refreshUrl = "http://localhost:" + port + "/auth/refresh";
        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(refreshUrl, refreshEntity, Map.class);
        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
        assertNotNull(refreshResponse.getBody().get("token"));
    }


    @Test
    void register_withExistingEmail_fails() {
        AuthRequest firstRegisterRequest = new AuthRequest();
        firstRegisterRequest.setEmail("test@example.com");
        firstRegisterRequest.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> firstRegisterEntity = new HttpEntity<>(firstRegisterRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> firstRegisterResponse = restTemplate.postForEntity(registerUrl, firstRegisterEntity, Map.class);
        assertEquals(HttpStatus.CREATED, firstRegisterResponse.getStatusCode());

        HttpEntity<AuthRequest> secondRegisterEntity = new HttpEntity<>(firstRegisterRequest, headers);
        ResponseEntity<Map> secondRegisterResponse = restTemplate.postForEntity(registerUrl, secondRegisterEntity, Map.class);

        assertEquals(HttpStatus.CONFLICT, secondRegisterResponse.getStatusCode());
        assertEquals("User with this email already exists.", secondRegisterResponse.getBody().get("error"));
    }

    @Test
    void login_withInvalidCredentials_fails() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrongpassword");
        HttpEntity<AuthRequest> loginEntity = new HttpEntity<>(loginRequest, headers);

        String loginUrl = "http://localhost:" + port + "/auth/login";
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(loginUrl, loginEntity, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, loginResponse.getStatusCode());
        assertEquals("Invalid username or password.", loginResponse.getBody().get("error"));
    }

    @Test
    void refreshToken_withInvalidToken_fails() {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshTokenUUID("8c18d8fa-0d92-4b4f-9c54-c6bf611cfd1b");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RefreshTokenRequest> refreshEntity = new HttpEntity<>(refreshRequest, headers);

        String refreshUrl = "http://localhost:" + port + "/auth/refresh";
        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(refreshUrl, refreshEntity, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, refreshResponse.getStatusCode());
        assertEquals("Refresh token not found or invalid.", refreshResponse.getBody().get("error"));
    }

}