package com.microservice.auth_service.service.authorization;

import com.microservice.auth_service.model.User;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.service.util.LocalizationService;
import com.microservice.auth_service.service.util.QRCodeService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TwoFactorAuthService {
    private final UserRepository userRepository;
    private final QRCodeService qrCodeService;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    private final LocalizationService localizationService;

    public String generateSecretKey(User user, Locale locale) {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        user.setTwoFASecret(key.getKey());
        user.set2FAEnabled(true);
        userRepository.save(user);
        return key.getKey();
    }

    public String getQRCode(User user, Locale locale) {
        if (user.getTwoFASecret() == null) {
            throw new IllegalStateException(localizationService.getMessage("error.2fa.not_activated", locale));
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
