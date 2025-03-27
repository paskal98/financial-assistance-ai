package com.microservice.auth_service.unit.controller;

import com.microservice.auth_service.controller.auth.OAuth2Controller;
import com.microservice.auth_service.service.authentication.OAuth2Service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ControllerTest {

    @Mock
    private OAuth2Service oAuth2Service;

    @InjectMocks
    private OAuth2Controller oAuth2Controller;

    @Test
    void oauthCallback_success() {
        OAuth2AuthenticationToken token = mock(OAuth2AuthenticationToken.class);
        String provider = "google";
        Locale locale = Locale.ENGLISH;
        Map<String, String> serviceResponse = Map.of("token", "jwt", "provider", provider, "message", "logged in");
        when(oAuth2Service.processOAuthCallback(token, provider, locale)).thenReturn(serviceResponse);

        Map<String, String> response = oAuth2Controller.oauthCallback(token, provider, locale);

        assertEquals(serviceResponse, response);
        verify(oAuth2Service, times(1)).processOAuthCallback(token, provider, locale);
    }
}