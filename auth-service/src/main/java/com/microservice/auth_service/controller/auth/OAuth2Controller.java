package com.microservice.auth_service.controller.auth;

import com.microservice.auth_service.service.authentication.OAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {
    private final OAuth2Service oAuth2Service;

    @GetMapping("/callback/{provider}")
    public Map<String, String> oauthCallback(@AuthenticationPrincipal OAuth2AuthenticationToken token,
                                             @PathVariable String provider,
                                             Locale locale) {
        return oAuth2Service.processOAuthCallback(token, provider, locale);
    }
}
