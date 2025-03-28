package com.microservice.balance_service.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
public class SavingsGoalResponse {
    private UUID id;
    private String name;
    private BigDecimal targetAmount;
    private BigDecimal currentAmount;

    public SavingsGoalResponse(UUID id, String name, BigDecimal targetAmount, BigDecimal currentAmount) {
        this.id = id;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
    }
}