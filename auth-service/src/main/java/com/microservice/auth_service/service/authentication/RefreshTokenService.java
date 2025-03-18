package com.microservice.auth_service.service.authentication;

import com.microservice.auth_service.model.RefreshToken;
import com.microservice.auth_service.model.User;
import com.microservice.auth_service.repository.RefreshTokenRepository;
import com.microservice.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration.refresh}")
    private long expirationMs;

    @Transactional
    public UUID createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID());
        refreshToken.setExpiryDate(Instant.now().plusSeconds(expirationMs));

        return refreshTokenRepository.save(refreshToken).getId();
    }


    public Optional<RefreshToken> findById(String id) {
        try {
            return refreshTokenRepository.findById(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }


    public boolean validateRefreshToken(String rawTokenUUID, RefreshToken storedToken) {
        return storedToken.getId().toString().equals(rawTokenUUID);
    }


    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void deleteExpiredTokens() {
        refreshTokenRepository.deleteAllByExpiryDateBefore(Instant.now());
    }
}
