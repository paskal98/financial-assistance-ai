package com.microservice.report_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {

    private UUID id;
    private BigDecimal amount;
    private String type;
    private String category;
    private String description;
    private String date;


}