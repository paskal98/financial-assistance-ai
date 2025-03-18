package com.microservice.auth_service.controller;

import com.microservice.auth_service.model.User;
import com.microservice.auth_service.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userOpt = userService.findByEmail(userDetails.getUsername());
        return userOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<Map<String, String>> enable2FA(@AuthenticationPrincipal UserDetails userDetails, Locale locale) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("error.user.not_found"));

        Map<String, String> response = userService.enable2FA(user, locale);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, String>> disable2FA(@AuthenticationPrincipal UserDetails userDetails, Locale locale) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("error.user.not_found"));

        Map<String, String> response = userService.disable2FA(user, locale);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/backup-codes")
    public ResponseEntity<Map<String, Object>> generateBackupCodes(@AuthenticationPrincipal UserDetails userDetails, Locale locale) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("error.user.not_found"));

        Map<String, Object> response = userService.generateBackupCodes(user, locale);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/2fa/disable-with-backup")
    public ResponseEntity<Map<String, String>> disable2FAWithBackupCode(@AuthenticationPrincipal UserDetails userDetails,
                                                                        @RequestBody Map<String, String> request,
                                                                        Locale locale) {
        User user = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("error.user.not_found"));

        Map<String, String> response = userService.disable2FAWithBackupCode(user, request.get("backupCode"), locale);
        HttpStatus status = response.containsKey("error") ? HttpStatus.BAD_REQUEST : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }
}
