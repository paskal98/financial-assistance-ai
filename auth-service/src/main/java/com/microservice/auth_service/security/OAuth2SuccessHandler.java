package com.microservice.auth_service.security;

import com.microservice.auth_service.model.entity.Role;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.RoleRepository;
import com.microservice.auth_service.repository.UserRepository;
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
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    @Value("${ip.frontend}")
    private String frontendUrlBase;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            throw new IllegalStateException("Ошибка: OAuth2 токен отсутствует!");
        }

        String provider = token.getAuthorizedClientRegistrationId();
        String email = token.getPrincipal().getAttribute("email");
        if (email == null) {
            throw new IllegalStateException("Ошибка: Email не найден!");
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;

        if (existingUser.isEmpty()) {
            user = new User();
            user.setEmail(email);
            user.setPassword("");
            user.setOauth(true);

            Role userRole = roleRepository.findByName("USER")
                    .orElseThrow(() -> new RuntimeException("Роль USER не найдена!"));
            user.setRoles(Set.of(userRole));

            user = userRepository.save(user);
        } else {
            user = existingUser.get();
        }

        String jwtToken = jwtUtil.generateToken(email, user.getId());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrlBase + "/oauth-success")
                .queryParam("provider", provider)
                .queryParam("token", jwtToken)
                .build().toUriString();

        response.sendRedirect(targetUrl);
    }
}
