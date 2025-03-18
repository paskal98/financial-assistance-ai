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
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret key is not configured properly!");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // ToDo - add security by claiming ip and userAgent
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
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
            throw new JwtValidationException("Token expired", (Collection<OAuth2Error>) e);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
            throw new JwtValidationException("Unsupported JWT", (Collection<OAuth2Error>) e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT: {}", e.getMessage());
            throw new JwtValidationException("Malformed JWT", (Collection<OAuth2Error>) e);
        } catch (SignatureException e) {
            log.warn("Invalid signature: {}", e.getMessage());
            throw new JwtValidationException("Invalid JWT signature", (Collection<OAuth2Error>) e);
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            throw new JwtValidationException("Invalid JWT", (Collection<OAuth2Error>) e);
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