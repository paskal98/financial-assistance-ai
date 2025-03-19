package com.microservice.document_processing_service.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionItemDto {
    private String name;
    private String category;
    private BigDecimal price;
    private BigDecimal vatAmount;
    private String date;
}