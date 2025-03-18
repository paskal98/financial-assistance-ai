package com.microservice.auth_service.service;

import com.microservice.auth_service.exception.AuthorizationExceptionHandler;
import com.microservice.auth_service.model.RefreshToken;
import com.microservice.auth_service.model.Role;
import com.microservice.auth_service.model.User;
import com.microservice.auth_service.repository.RoleRepository;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final TwoFactorAuthService twoFactorAuthService;

    public Map<String, String> register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AuthorizationExceptionHandler.UserAlreadyExistsException("Email уже используется");
        }

        // Ищем роль USER
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new AuthorizationExceptionHandler.RoleNotFoundException("Роль USER не найдена в БД"));

        // Создаем пользователя
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        String token = jwtUtil.generateToken(email);
        String refreshToken = String.valueOf(refreshTokenService.createRefreshToken(user));

        return Map.of("token", token, "refreshToken", refreshToken);
    }


    public Map<String, String> login(String email, String password, Integer otpCode) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new AuthorizationExceptionHandler.InvalidCredentialsException("Неверный логин или пароль");
        }

        User user = userOpt.get();

        if (user.isOauth()) {
            throw new AuthorizationExceptionHandler.InvalidCredentialsException("Используйте вход через OAuth");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthorizationExceptionHandler.InvalidCredentialsException("Неверный логин или пароль");
        }

        if (user.is2FAEnabled()) {
            if (otpCode == null || !twoFactorAuthService.verifyCode(user, otpCode)) {
                throw new AuthorizationExceptionHandler.InvalidCredentialsException("Неверный 2FA-код");
            }
        }

        String token = jwtUtil.generateToken(email);
        String refreshToken = String.valueOf(refreshTokenService.createRefreshToken(user));

        return Map.of("token", token, "refreshToken", refreshToken);
    }



    @Transactional
    public Map<String, String> refreshToken(String refreshTokenUUID) {
        // Ищем токен в базе
        Optional<RefreshToken> tokenOpt = refreshTokenService.findById(refreshTokenUUID);
        if (tokenOpt.isEmpty()) {
            throw new RuntimeException("Refresh-токен не найден или некорректен");
        }

        RefreshToken storedToken = tokenOpt.get();
        User user = storedToken.getUser();

        // Проверяем срок действия
        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenService.deleteByUser(user);
            throw new RuntimeException("Refresh-токен истек, пожалуйста, войдите снова");
        }

        // Сравниваем rawToken с хешированным токеном в БД
        if (!refreshTokenService.validateRefreshToken(refreshTokenUUID, storedToken)) {
            throw new RuntimeException("Неверный refresh-токен");
        }

        // Генерируем новый Access Token
        String newAccessToken = jwtUtil.generateToken(user.getEmail());

        return Map.of("token", newAccessToken);
    }



    //ToDo - add check
    public void logout(String userEmail) {
        User user = userRepository.findByEmail(userEmail).get();
        refreshTokenService.deleteByUser(user);
    }
}
