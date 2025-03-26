package com.microservice.auth_service.controller.auth;

import com.microservice.auth_service.model.dto.auth.AuthRequest;
import com.microservice.auth_service.model.dto.auth.RefreshTokenRequest;
import com.microservice.auth_service.service.authentication.AuthService;
import com.microservice.auth_service.service.util.LocalizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final LocalizationService localizationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody AuthRequest request, Locale locale) {
        Map<String, String> response = authService.register(request.getEmail(), request.getPassword(), locale);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthRequest request, Locale locale) {
        Map<String, String> response = authService.login(request, locale);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(@Valid @RequestBody RefreshTokenRequest request, Locale locale) {
        Map<String, String> response = authService.refreshToken(request.getRefreshTokenUUID(), locale);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> request, Locale locale) {
        authService.logout(request.get("email"), locale);
        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("message", localizationService.getMessage("success.logout", locale)));
    }
}
