package com.microservice.auth_service.service.authentication;

import com.microservice.auth_service.model.dto.auth.AuthRequest;
import com.microservice.auth_service.middleware.exception.AuthorizationExceptionHandler;
import com.microservice.auth_service.model.entity.RefreshToken;
import com.microservice.auth_service.model.entity.Role;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.RoleRepository;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.security.JwtUtil;
import com.microservice.auth_service.service.util.LocalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
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
    private final BackupCodeService backupCodeService;
    private final LocalizationService localizationService;

    public Map<String, String> register(String email, String password, Locale locale) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new AuthorizationExceptionHandler.UserAlreadyExistsException(localizationService.getMessage("error.user.exists", locale));
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new AuthorizationExceptionHandler.RoleNotFoundException(localizationService.getMessage("error.role.not_found", locale)));

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Set.of(userRole));

        user = userRepository.save(user);

        String token = jwtUtil.generateToken(email, user.getId());
        String refreshToken = String.valueOf(refreshTokenService.createRefreshToken(user));

        return Map.of(
                "token", token,
                "refreshToken", refreshToken,
                "message", localizationService.getMessage("success.register", locale)
        );
    }

    public Map<String, String> login(AuthRequest request, Locale locale) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isEmpty()) {
            throw new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.invalid.credentials", locale));
        }

        User user = userOpt.get();

        if (user.isOauth()) {
            throw new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.oauth.use", locale));
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.invalid.credentials", locale));
        }

        if (user.is2FAEnabled()) {
            if (request.getBackupCode() != null) {
                if (!backupCodeService.validateBackupCode(user, request.getBackupCode())) {
                    throw new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.2fa.invalid", locale));
                }
            } else if (request.getOtpCode() != null) {
                if (!twoFactorAuthService.verifyCode(user, request.getOtpCode())) {
                    throw new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.2fa.invalid", locale));
                }
            } else {
                throw new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.2fa.required", locale));
            }
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        String refreshToken = String.valueOf(refreshTokenService.createRefreshToken(user));

        return Map.of(
                "token", token,
                "refreshToken", refreshToken,
                "message", localizationService.getMessage("success.login", locale)
        );
    }

    @Transactional
    public Map<String, String> refreshToken(String refreshTokenUUID, Locale locale) {
        Optional<RefreshToken> tokenOpt = refreshTokenService.findById(refreshTokenUUID);
        if (tokenOpt.isEmpty()) {
            throw new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.refresh_token.not_found", locale));
        }

        RefreshToken storedToken = tokenOpt.get();
        User user = storedToken.getUser();

        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenService.deleteByUser(user);
            throw new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.refresh_token.expired", locale));
        }

        if (!refreshTokenService.validateRefreshToken(refreshTokenUUID, storedToken)) {
            throw new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.refresh_token.invalid", locale));
        }

        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getId());

        return Map.of(
                "token", newAccessToken,
                "message", localizationService.getMessage("success.token.refreshed", locale)
        );
    }

    public void logout(String userEmail, Locale locale) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AuthorizationExceptionHandler.InvalidCredentialsException(localizationService.getMessage("error.user.not_found", locale)));

        refreshTokenService.deleteByUser(user);
    }
}
