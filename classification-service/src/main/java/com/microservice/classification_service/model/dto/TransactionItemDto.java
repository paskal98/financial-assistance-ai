package com.microservice.classification_service.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class TransactionItemDto {
    private String name;
    private String category;
    private String type;
    private BigDecimal price;
    private Instant date;
    private String description;
    private String paymentMethod;
    private UUID documentId;
    private UUID userId;
}