package com.microservice.balance_service.service;

import com.microservice.balance_service.model.dto.SavingsGoalRequest;
import com.microservice.balance_service.model.dto.SavingsGoalResponse;
import com.microservice.balance_service.model.dto.UserBalanceResponse;

import java.util.List;
import java.util.UUID;

public interface BalanceService {
    UserBalanceResponse getUserBalanceResponse(UUID userId);
    SavingsGoalResponse createSavingsGoal(UUID userId, SavingsGoalRequest request);
    List<SavingsGoalResponse> getSavingsGoals(UUID userId);
}