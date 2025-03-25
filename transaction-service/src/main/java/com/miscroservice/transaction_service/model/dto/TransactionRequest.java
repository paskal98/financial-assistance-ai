package com.miscroservice.transaction_service.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @NotNull(message = "Amount cannot be null")
    private BigDecimal amount;

    @NotBlank(message = "Type cannot be blank")
    private String type;

    @NotBlank(message = "Category cannot be blank")
    private String category;

    private String description;

    @NotBlank(message = "Date cannot be blank")
    private String date;

    private String paymentMethod;
}