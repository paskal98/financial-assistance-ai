package com.microservice.auth_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    public String secret;

    private SecretKey secretKey;

    @Value("${jwt.expiration}")
    public long expirationMs;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret key is not configured properly!");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // ToDo - add security by claiming ip and userAgent
    public String generateToken(String email, UUID userId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId.toString())
//                .claim("ip", ip)
//                .claim("userAgent", userAgent)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }


    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }



    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired at: {}", e.getClaims().getExpiration());
            OAuth2Error error = new OAuth2Error("token_expired", "Token expired at " + e.getClaims().getExpiration(), null);
            throw new JwtValidationException("Token expired", List.of(error));
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
            OAuth2Error error = new OAuth2Error("unsupported_jwt", "Unsupported JWT", null);
            throw new JwtValidationException("Unsupported JWT", List.of(error));
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT: {}", e.getMessage());
            OAuth2Error error = new OAuth2Error("malformed_jwt", "Malformed JWT", null);
            throw new JwtValidationException("Malformed JWT", List.of(error));
        } catch (SignatureException e) {
            log.warn("Invalid signature: {}", e.getMessage());
            OAuth2Error error = new OAuth2Error("invalid_signature", "Invalid JWT signature", null);
            throw new JwtValidationException("Invalid JWT signature", List.of(error));
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            OAuth2Error error = new OAuth2Error("invalid_jwt", "Invalid JWT", null);
            throw new JwtValidationException("Invalid JWT", List.of(error));
        }
    }


    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }
}