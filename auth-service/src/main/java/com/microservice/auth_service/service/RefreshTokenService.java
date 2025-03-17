package com.microservice.auth_service.service;

import com.microservice.auth_service.model.RefreshToken;
import com.microservice.auth_service.model.User;
import com.microservice.auth_service.repository.RefreshTokenRepository;
import com.microservice.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    public RefreshToken createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(hashedToken);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(604800)); // 7 дней

        refreshToken = refreshTokenRepository.save(refreshToken);

        // Отдаем клиенту оригинальный (не хешированный) refreshToken
        refreshToken.setToken(rawToken);
        return refreshToken;
    }

    public Optional<RefreshToken> findById(String id) {
        try {
            return refreshTokenRepository.findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty(); // Если UUID некорректен, возвращаем пустой Optional
        }
    }


    public boolean validateRefreshToken(String rawTokenUUID, RefreshToken storedToken) {
        return storedToken.getId().toString().equals(rawTokenUUID);
    }


    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
