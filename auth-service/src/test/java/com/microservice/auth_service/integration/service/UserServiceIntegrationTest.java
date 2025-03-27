package com.microservice.auth_service.integration.service;

import com.microservice.auth_service.integration.BaseIntegrationTest;
import com.microservice.auth_service.model.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Test
    void enable2FA_integration() {
        User user = createUser("test@example.com", "password", Set.of("USER"));

        Map<String, String> response = userService.enable2FA(user, Locale.ENGLISH);

        assertNotNull(response.get("secret"));
        assertNotNull(response.get("qrCode"));
        assertEquals("Two-factor authentication has been enabled.", response.get("message"));

        User updatedUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assertTrue(updatedUser.is2FAEnabled());
        assertNotNull(updatedUser.getTwoFASecret());
    }

    @Test
    void generateBackupCodes_integration() {
        User user = createUser("test@example.com", "password", Set.of("USER"));

        Map<String, Object> response = userService.generateBackupCodes(user, Locale.ENGLISH);

        assertNotNull(response.get("backupCodes"));
        assertEquals("Backup codes have been generated successfully.", response.get("message"));

        User updatedUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assertFalse(updatedUser.getBackupCodes().isEmpty());
    }
}
