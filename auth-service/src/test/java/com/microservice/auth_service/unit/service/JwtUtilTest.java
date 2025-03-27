package com.microservice.auth_service.unit.service;

import com.microservice.auth_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        jwtUtil.secret = "AAAAcwAAAGUAAABjAAAAcgAAAGUAAAB0AAAAXwAAAGsAAABlAAAAeQ==";
        jwtUtil.expirationMs = 86400000;
        jwtUtil.init();
    }

    @Test
    void generateToken_and_validate_success() {
        String token = jwtUtil.generateToken("test@example.com", UUID.randomUUID());

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    void validateToken_expired_throwsException() {
        jwtUtil.expirationMs = -1000; // Устанавливаем истекший токен
        String token = jwtUtil.generateToken("test@example.com", UUID.randomUUID());

        assertThrows(JwtValidationException.class, () -> jwtUtil.validateToken(token));
    }
}