package com.microservice.balance_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SavingsGoalRequest {
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull(message = "Target amount cannot be null")
    private BigDecimal targetAmount;
}