package com.microservice.auth_service.controller;

import com.microservice.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {
    private final JwtUtil jwtUtil;

    @GetMapping("/callback/{provider}")
    public Map<String, String> oauthCallback(@AuthenticationPrincipal OAuth2AuthenticationToken token, @PathVariable String provider) {
        if (token == null) {
            throw new IllegalStateException("Ошибка: OAuth2 токен отсутствует!");
        }

        String email = token.getPrincipal().getAttribute("email");
        if (email == null) {
            throw new IllegalStateException("Ошибка: Email не найден!");
        }

        String jwtToken = jwtUtil.generateToken(email);

        return Map.of(
                "provider", provider,
                "email", email,
                "token", jwtToken
        );
    }
}
