package com.microservice.auth_service.service.authorization;

import com.microservice.auth_service.security.JwtUtil;
import com.microservice.auth_service.service.util.LocalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2Service {
    private final JwtUtil jwtUtil;
    private final LocalizationService localizationService;

    public Map<String, String> processOAuthCallback(OAuth2AuthenticationToken token, String provider, Locale locale) {
        if (token == null) {
            throw new IllegalStateException(localizationService.getMessage("error.oauth.token_missing", locale));
        }

        String email = token.getPrincipal().getAttribute("email");
        if (email == null) {
            throw new IllegalStateException(localizationService.getMessage("error.oauth.email_not_found", locale));
        }

        String jwtToken = jwtUtil.generateToken(email);

        return Map.of(
                "message", localizationService.getMessage("success.oauth.login", locale, provider),
                "provider", provider,
                "email", email,
                "token", jwtToken
        );
    }
}
