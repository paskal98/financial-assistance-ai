package com.microservice.auth_service.service.user;

import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.service.authentication.BackupCodeService;
import com.microservice.auth_service.service.util.LocalizationService;
import com.microservice.auth_service.service.authentication.TwoFactorAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final LocalizationService localizationService;
    private final BackupCodeService backupCodeService;
    private final TwoFactorAuthService twoFactorAuthService;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public Map<String, String> enable2FA(User user, Locale locale) {
        String secret = twoFactorAuthService.generateSecretKey(user, locale);
        String qrCode = twoFactorAuthService.getQRCode(user, locale);

        return Map.of(
                "message", localizationService.getMessage("success.2fa.enabled", locale),
                "secret", secret,
                "qrCode", qrCode
        );
    }

    @Transactional
    public Map<String, String> disable2FA(User user, Locale locale) {
        twoFactorAuthService.disable2FA(user);
        return Map.of("message", localizationService.getMessage("success.2fa.disabled", locale));
    }

    @Transactional
    public Map<String, Object> generateBackupCodes(User user, Locale locale) {
        List<String> backupCodes = backupCodeService.generateBackupCodes(user);
        return Map.of(
                "message", localizationService.getMessage("success.2fa.backup_generated", locale),
                "backupCodes", backupCodes
        );
    }

    @Transactional
    public Map<String, String> disable2FAWithBackupCode(User user, String backupCode, Locale locale) {
        if (backupCodeService.validateBackupCode(user, backupCode)) {
            user.set2FAEnabled(false);
            user.setTwoFASecret(null);
            userRepository.save(user);
            return Map.of("message", localizationService.getMessage("success.2fa.disabled_backup", locale));
        } else {
            return Map.of("error", localizationService.getMessage("error.2fa.invalid_backup", locale));
        }
    }
}
