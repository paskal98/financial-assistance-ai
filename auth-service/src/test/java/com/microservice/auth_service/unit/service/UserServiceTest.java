package com.microservice.auth_service.unit.service;

import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.service.authentication.BackupCodeService;
import com.microservice.auth_service.service.authentication.TwoFactorAuthService;
import com.microservice.auth_service.service.user.UserService;
import com.microservice.auth_service.service.util.LocalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private BackupCodeService backupCodeService;

    @Mock
    private TwoFactorAuthService twoFactorAuthService;

    @InjectMocks
    private UserService userService;

    private User user;
    private Locale locale;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID(), "test@example.com", "encodedPassword", false, Set.of(), false, null, null);
        locale = Locale.ENGLISH;
    }

    @Test
    void enable2FA_success() {
        // Arrange
        when(twoFactorAuthService.generateSecretKey(user, locale)).thenReturn("generatedSecret");
        when(twoFactorAuthService.getQRCode(user, locale)).thenReturn("base64QRCode");
        when(localizationService.getMessage("success.2fa.enabled", locale))
                .thenReturn("Two-factor authentication has been enabled.");

        // Act
        Map<String, String> response = userService.enable2FA(user, locale);

        // Assert
        assertEquals("generatedSecret", response.get("secret"));
        assertEquals("base64QRCode", response.get("qrCode"));
        assertEquals("Two-factor authentication has been enabled.", response.get("message"));

        verify(twoFactorAuthService, times(1)).generateSecretKey(user, locale);
        verify(twoFactorAuthService, times(1)).getQRCode(user, locale);

    }

    @Test
    void disable2FA_success() {
        when(localizationService.getMessage("success.2fa.disabled", locale)).thenReturn("2FA disabled");

        Map<String, String> result = userService.disable2FA(user, locale);

        assertEquals("2FA disabled", result.get("message"));
        verify(twoFactorAuthService).disable2FA(user);
    }

    @Test
    void generateBackupCodes_success() {
        List<String> codes = List.of("code1", "code2");
        when(backupCodeService.generateBackupCodes(user)).thenReturn(codes);
        when(localizationService.getMessage("success.2fa.backup_generated", locale)).thenReturn("Backup codes generated");

        Map<String, Object> result = userService.generateBackupCodes(user, locale);

        assertEquals(codes, result.get("backupCodes"));
        assertEquals("Backup codes generated", result.get("message"));
    }

    @Test
    void disable2FAWithBackupCode_success() {
        when(backupCodeService.validateBackupCode(user, "code1")).thenReturn(true);
        when(localizationService.getMessage("success.2fa.disabled_backup", locale)).thenReturn("2FA disabled with backup");

        Map<String, String> result = userService.disable2FAWithBackupCode(user, "code1", locale);

        assertEquals("2FA disabled with backup", result.get("message"));
        verify(userRepository).save(user);
    }

    @Test
    void disable2FAWithBackupCode_invalidCode() {
        when(backupCodeService.validateBackupCode(user, "invalidCode")).thenReturn(false);
        when(localizationService.getMessage("error.2fa.invalid_backup", locale)).thenReturn("Invalid backup code");

        Map<String, String> result = userService.disable2FAWithBackupCode(user, "invalidCode", locale);

        assertEquals("Invalid backup code", result.get("error"));
        verify(userRepository, never()).save(any(User.class));
    }
}