package com.microservice.report_service.model.entity;

import com.microservice.report_service.model.dto.TransactionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummary {
    private BigDecimal totalAmount;
    private BigDecimal averagePerDay;
    private String topCategory;
    private LocalDate topDay;
    private int activeDaysCount;
    private int uniqueCategoriesCount;
    private List<TransactionResponse> topTransactions;

    private BigDecimal previousTotalAmount;
    private BigDecimal previousAveragePerDay;
    private BigDecimal deltaPercentage;
}
