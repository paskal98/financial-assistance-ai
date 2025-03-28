package com.microservice.balance_service.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
public class UserBalanceResponse {
    private UUID userId;
    private BigDecimal balance;

    public UserBalanceResponse(UUID userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }
}