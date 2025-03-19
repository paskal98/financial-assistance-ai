package com.miscroservice.transaction_service.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
public class TransactionResponse {

    private UUID id;
    private BigDecimal amount;
    private String type;
    private String category;
    private String description;
    private String date;

    public TransactionResponse(UUID id, BigDecimal amount, String type, String category, String description, String date) {
        this.id = id;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.date = date;
    }
}