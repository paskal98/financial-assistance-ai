package com.microservice.balance_service.controller;

import com.microservice.balance_service.model.dto.UserBalanceResponse;
import com.microservice.balance_service.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/balances")
@RequiredArgsConstructor
public class BalanceController {
    private final BalanceService balanceService;

    @GetMapping
    public ResponseEntity<UserBalanceResponse> getBalance(@AuthenticationPrincipal String userId) {
        UserBalanceResponse balance = balanceService.getUserBalanceResponse(UUID.fromString(userId));
        return ResponseEntity.ok(balance);
    }
}