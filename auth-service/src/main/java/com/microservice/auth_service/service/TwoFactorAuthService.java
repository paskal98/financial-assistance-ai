package com.microservice.auth_service.service;

import com.microservice.auth_service.model.User;
import com.microservice.auth_service.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {
    private final UserRepository userRepository;
    private final QRCodeService qrCodeService ;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    public String generateSecretKey(User user) {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        user.setTwoFASecret(key.getKey()); // Сохраняем только строку ключа
        user.set2FAEnabled(true);
        userRepository.save(user);
        return key.getKey();
    }

    public String getQRCode(User user) {
        if (user.getTwoFASecret() == null) {
            throw new IllegalStateException("2FA не активирована для пользователя");
        }

        String otpAuthUrl = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                "FinanceAssistant", user.getEmail(), new GoogleAuthenticatorKey.Builder(user.getTwoFASecret()).build()
        );

        return qrCodeService.generateQRCodeBase64(otpAuthUrl);
    }

    public boolean verifyCode(User user, int code) {
        return googleAuthenticator.authorize(user.getTwoFASecret(), code);
    }

    public void disable2FA(User user) {
        user.set2FAEnabled(false);
        user.setTwoFASecret(null);
        userRepository.save(user);
    }
}
