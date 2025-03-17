package com.microservice.auth_service.service;

import com.microservice.auth_service.model.RefreshToken;
import com.microservice.auth_service.model.User;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public Map<String, String> register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email уже используется");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        String token = jwtUtil.generateToken(email);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return Map.of("token", token, "refreshToken", refreshToken);
    }

    public Map<String, String> login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            throw new RuntimeException("Неверный логин или пароль");
        }

        String token = jwtUtil.generateToken(email);
        String refreshToken = refreshTokenService.createRefreshToken(userOpt.get()).getToken();

        return Map.of("token", token, "refreshToken", refreshToken);
    }

    public Map<String, String> refreshToken(String refreshToken) {
        Optional<RefreshToken> tokenOpt = refreshTokenService.findByToken(refreshToken);
        if (tokenOpt.isEmpty() || tokenOpt.get().getExpiryDate().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh-токен недействителен");
        }

        String newToken = jwtUtil.generateToken(tokenOpt.get().getUser().getEmail());
        return Map.of("token", newToken);
    }

    //ToDo - add check
    public void logout(String userEmail) {
        User user = userRepository.findByEmail(userEmail).get();
        refreshTokenService.deleteByUser(user);
    }
}
