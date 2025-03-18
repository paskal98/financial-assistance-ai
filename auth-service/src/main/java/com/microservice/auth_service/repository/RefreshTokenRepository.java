package com.microservice.auth_service.repository;

import com.microservice.auth_service.model.RefreshToken;
import com.microservice.auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findById(UUID id);
    void deleteByUser(User user);
    void deleteAllByExpiryDateBefore(Instant time);
}