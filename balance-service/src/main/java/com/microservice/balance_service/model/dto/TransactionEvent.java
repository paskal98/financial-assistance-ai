package com.microservice.balance_service.model.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransactionEvent {
    private String transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String type;
    private String operation;
    private BigDecimal oldAmount;
    private String oldType;

    public static TransactionEvent fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, TransactionEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize TransactionEvent", e);
        }
    }
}