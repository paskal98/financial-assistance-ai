package com.miscroservice.transaction_service.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class TransactionStatsResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private Map<String, BigDecimal> byCategory;
}