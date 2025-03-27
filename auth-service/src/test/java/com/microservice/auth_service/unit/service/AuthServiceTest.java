package com.microservice.auth_service.unit.service;

import com.microservice.auth_service.middleware.exception.AuthorizationExceptionHandler;
import com.microservice.auth_service.model.dto.auth.AuthRequest;
import com.microservice.auth_service.model.entity.RefreshToken;
import com.microservice.auth_service.model.entity.Role;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.RoleRepository;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.security.JwtUtil;
import com.microservice.auth_service.service.authentication.AuthService;
import com.microservice.auth_service.service.authentication.BackupCodeService;
import com.microservice.auth_service.service.authentication.RefreshTokenService;
import com.microservice.auth_service.service.authentication.TwoFactorAuthService;
import com.microservice.auth_service.service.util.LocalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    @Mock
    private BackupCodeService backupCodeService;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private Locale locale;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID(), "test@example.com", "encodedPassword", false, Set.of(new Role(1, "USER")), false, null, null);
        locale = Locale.ENGLISH;
    }

    @Test
    void register_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role(1, "USER")));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtUtil.generateToken("test@example.com", user.getId())).thenReturn("jwtToken");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(UUID.randomUUID());
        when(localizationService.getMessage("success.register", locale)).thenReturn("Registration successful");

        Map<String, String> result = authService.register("test@example.com", "password", locale);

        assertEquals("jwtToken", result.get("token"));
        assertNotNull(result.get("refreshToken"));
        assertEquals("Registration successful", result.get("message"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_userAlreadyExists_throwsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(localizationService.getMessage("error.user.exists", locale)).thenReturn("User already exists");

        assertThrows(AuthorizationExceptionHandler.UserAlreadyExistsException.class, () ->
                authService.register("test@example.com", "password", locale));
    }

    @Test
    void login_success() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com", user.getId())).thenReturn("jwtToken");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(UUID.randomUUID());
        when(localizationService.getMessage("success.login", locale)).thenReturn("Login successful");

        Map<String, String> result = authService.login(request, locale);

        assertEquals("jwtToken", result.get("token"));
        assertNotNull(result.get("refreshToken"));
        assertEquals("Login successful", result.get("message"));
    }

    @Test
    void login_invalidCredentials_throwsException() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);
        when(localizationService.getMessage("error.invalid.credentials", locale)).thenReturn("Invalid credentials");

        assertThrows(AuthorizationExceptionHandler.InvalidCredentialsException.class, () ->
                authService.login(request, locale));
    }

    @Test
    void refreshToken_success() {
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user, UUID.randomUUID(), Instant.now().plusSeconds(3600));
        when(refreshTokenService.findById(anyString())).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.validateRefreshToken(anyString(), eq(refreshToken))).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com", user.getId())).thenReturn("newJwtToken");
        when(localizationService.getMessage("success.token.refreshed", locale)).thenReturn("Token refreshed");

        Map<String, String> result = authService.refreshToken(refreshToken.getId().toString(), locale);

        assertEquals("newJwtToken", result.get("token"));
        assertEquals("Token refreshed", result.get("message"));
    }

    @Test
    void refreshToken_expired_throwsException() {
        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user, UUID.randomUUID(), Instant.now().minusSeconds(3600));
        when(refreshTokenService.findById(anyString())).thenReturn(Optional.of(refreshToken));
        when(localizationService.getMessage("error.refresh_token.expired", locale)).thenReturn("Refresh token expired");

        assertThrows(AuthorizationExceptionHandler.InvalidCredentialsException.class, () ->
                authService.refreshToken(refreshToken.getId().toString(), locale));
        verify(refreshTokenService).deleteByUser(user);
    }

    @Test
    void logout_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        authService.logout("test@example.com", locale);
        verify(refreshTokenService).deleteByUser(user);
    }
}