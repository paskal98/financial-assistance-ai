package com.microservice.auth_service.controller;

import com.microservice.auth_service.model.User;
import com.microservice.auth_service.service.TwoFactorAuthService;
import com.microservice.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final TwoFactorAuthService twoFactorAuthService;

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

}
