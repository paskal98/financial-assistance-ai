package com.microservice.auth_service.controller;

import com.microservice.auth_service.dto.AuthRequest;
import com.microservice.auth_service.dto.RefreshTokenRequest;
import com.microservice.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public Map<String, String> register(@Valid @RequestBody AuthRequest request) {
        return authService.register(request.getEmail(), request.getPassword());
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request.getEmail(), request.getPassword());
    }

    @PostMapping("/refresh")
    public Map<String, String> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshTokenUUID());
    }

    @PostMapping("/logout")
    public void logout(@RequestBody Map<String, String> request) {
        authService.logout(request.get("email"));
    }
}
