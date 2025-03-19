package com.miscroservice.transaction_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @NotNull
    private BigDecimal amount;

    @NotBlank
    private String type;

    @NotBlank
    private String category;

    private String description;

    @NotBlank
    private String date;

    private String paymentMethod;
}