package com.microservice.auth_service.controller;

import com.microservice.auth_service.model.User;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.service.BackupCodeService;
import com.microservice.auth_service.service.TwoFactorAuthService;
import com.microservice.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final BackupCodeService backupCodeService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserRepository userRepository;


    @GetMapping("/me")
    public Optional<User> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.findByEmail(userDetails.getUsername());
    }

    @PostMapping("/2fa/enable")
    public Map<String, String> enable2FA(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        String secret = twoFactorAuthService.generateSecretKey(user);
        String qrCode = twoFactorAuthService.getQRCode(user);
        return Map.of("secret", secret, "qrCode", qrCode);
    }

    @PostMapping("/2fa/disable")
    public Map<String, String> disable2FA(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        twoFactorAuthService.disable2FA(user);
        return Map.of("message", "2FA отключена");
    }

    @PostMapping("/2fa/backup-codes")
    public List<String> generateBackupCodes(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        return backupCodeService.generateBackupCodes(user);
    }

    @PostMapping("/2fa/disable-with-backup")
    public Map<String, String> disable2FAWithBackupCode(@AuthenticationPrincipal UserDetails userDetails, @RequestBody Map<String, String> request) {
        User user = userService.findByEmail(userDetails.getUsername()).orElseThrow();
        String backupCode = request.get("backupCode");

        if (backupCodeService.validateBackupCode(user, backupCode)) {
            user.set2FAEnabled(false);
            user.setTwoFASecret(null);
            userRepository.save(user);
            return Map.of("message", "2FA отключена с использованием резервного кода");
        } else {
            return Map.of("error", "Неверный резервный код");
        }
    }

}
