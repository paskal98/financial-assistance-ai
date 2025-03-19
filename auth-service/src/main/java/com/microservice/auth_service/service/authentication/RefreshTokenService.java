package com.microservice.auth_service.service.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.auth_service.model.entity.RefreshToken;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper; // JSON-сериализация

    @Value("${jwt.expiration.refresh}")
    private long expirationMs;

    /**
     * Создает новый Refresh-токен, удаляя старый.
     */
    @Transactional
    public UUID createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID());
        refreshToken.setExpiryDate(Instant.now().plusMillis(expirationMs));

        // Сохраняем в PostgreSQL
        refreshTokenRepository.save(refreshToken);

        // Кладём в Redis (сериализуем в JSON)
        try {
            String refreshTokenJson = objectMapper.writeValueAsString(refreshToken);
            redisTemplate.opsForValue().set("refresh:" + refreshToken.getToken().toString(),
                    refreshTokenJson, expirationMs, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка сериализации RefreshToken в Redis", e);
        }

        return refreshToken.getId();
    }

    /**
     * Ищет Refresh-токен сначала в Redis, затем в PostgreSQL.
     */
    public Optional<RefreshToken> findById(String token) {
        // 1️⃣ Проверяем Redis
        String refreshTokenJson = redisTemplate.opsForValue().get("refresh:" + token);
        if (refreshTokenJson != null) {
            try {
                RefreshToken refreshToken = objectMapper.readValue(refreshTokenJson, RefreshToken.class);
                return Optional.of(refreshToken);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Ошибка десериализации RefreshToken из Redis", e);
            }
        }

        // 2️⃣ Если нет в Redis → ищем в PostgreSQL
        Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findById(UUID.fromString(token));

        refreshTokenOpt.ifPresent(rt -> {
            try {
                String json = objectMapper.writeValueAsString(rt);
                redisTemplate.opsForValue().set("refresh:" + rt.getToken().toString(), json,
                        expirationMs, TimeUnit.MILLISECONDS);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Ошибка сериализации RefreshToken в Redis", e);
            }
        });

        return refreshTokenOpt;
    }

    /**
     * Проверяет валидность Refresh-токена.
     */
    public boolean validateRefreshToken(String rawTokenUUID, RefreshToken storedToken) {
        return storedToken.getId().toString().equals(rawTokenUUID);
    }

    /**
     * Удаляет все Refresh-токены пользователя (из PostgreSQL и Redis).
     */
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);

        // Удаляем из Redis только токены текущего пользователя
        Set<String> keys = redisTemplate.keys("refresh:*");
        if (keys != null) {
            keys.forEach(key -> {
                String refreshTokenJson = redisTemplate.opsForValue().get(key);
                if (refreshTokenJson != null) {
                    try {
                        RefreshToken rt = objectMapper.readValue(refreshTokenJson, RefreshToken.class);
                        if (rt.getUser().getId().equals(user.getId())) {
                            redisTemplate.delete(key);
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Ошибка десериализации RefreshToken при удалении", e);
                    }
                }
            });
        }
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void deleteExpiredTokens() {
        // Получаем все истекшие токены
        refreshTokenRepository.findAllByExpiryDateBefore(Instant.now()).forEach(rt -> {
            refreshTokenRepository.delete(rt);
            redisTemplate.delete("refresh:" + rt.getToken().toString()); // Удаляем из Redis
        });
    }
}

