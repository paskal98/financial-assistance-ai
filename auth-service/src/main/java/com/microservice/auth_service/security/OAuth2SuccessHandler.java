package com.microservice.auth_service.security;

import com.microservice.auth_service.model.User;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${ip.frontend}")
    private String frontendUrlBase;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            throw new IllegalStateException("Ошибка: OAuth2 токен отсутствует!");
        }

        String email = token.getPrincipal().getAttribute("email");
        if (email == null) {
            throw new IllegalStateException("Ошибка: Email не найден в профиле пользователя!");
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPassword("");
            userRepository.save(newUser);
        }

        String jwtToken = jwtUtil.generateToken(email);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrlBase+"/oauth-success")
                .queryParam("token", jwtToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}
