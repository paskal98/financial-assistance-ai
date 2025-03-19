package com.miscroservice.transaction_service.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
public class TransactionStatsResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private Map<String, BigDecimal> byCategory;
    private Map<String, BigDecimal> byMonth;
}