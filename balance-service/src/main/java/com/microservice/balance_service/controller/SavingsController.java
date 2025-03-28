package com.microservice.balance_service.controller;


import com.microservice.balance_service.model.dto.SavingsGoalRequest;
import com.microservice.balance_service.model.dto.SavingsGoalResponse;
import com.microservice.balance_service.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/savings")
@RequiredArgsConstructor
public class SavingsController {
    private final BalanceService balanceService;

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> createSavingsGoal(
            @RequestBody SavingsGoalRequest request,
            @AuthenticationPrincipal String userId) {
        SavingsGoalResponse response = balanceService.createSavingsGoal(UUID.fromString(userId), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SavingsGoalResponse>> getSavingsGoals(@AuthenticationPrincipal String userId) {
        List<SavingsGoalResponse> goals = balanceService.getSavingsGoals(UUID.fromString(userId));
        return ResponseEntity.ok(goals);
    }
}