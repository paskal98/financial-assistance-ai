package com.microservice.auth_service.controller;

import com.microservice.auth_service.dto.AuthRequest;
import com.microservice.auth_service.dto.RefreshTokenRequest;
import com.microservice.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody AuthRequest request) {
        Map<String, String> response = authService.register(request.getEmail(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthRequest request) {
        Map<String, String> response = authService.login(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        Map<String, String> response = authService.refreshToken(request.getRefreshTokenUUID());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> request) {
        authService.logout(request.get("email"));
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message","Вы успешно вышли из системы"));
    }
}
