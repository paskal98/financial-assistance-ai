package com.microservice.report_service.service.analytics;

import com.microservice.report_service.model.dto.TransactionResponse;
import com.microservice.report_service.model.entity.ReportSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportAnalyticsService {

    public ReportSummary analyze(List<TransactionResponse> current, List<TransactionResponse> previous) {
        BigDecimal total = sum(current);

        Map<LocalDate, BigDecimal> dailyTotals = groupByDay(current);

        BigDecimal averagePerDay = dailyTotals.isEmpty()
                ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(dailyTotals.size()), RoundingMode.HALF_UP);

        String topCategory = current.stream()
                .collect(Collectors.groupingBy(TransactionResponse::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, TransactionResponse::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        LocalDate topDay = dailyTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        List<TransactionResponse> top5Transactions = current.stream()
                .sorted(Comparator.comparing(TransactionResponse::getAmount).reversed())
                .limit(5)
                .toList();

        int activeDays = dailyTotals.size();
        long uniqueCategories = current.stream()
                .map(TransactionResponse::getCategory)
                .distinct()
                .count();

        // Предыдущий период
        BigDecimal prevTotal = sum(previous);
        Map<LocalDate, BigDecimal> prevDailyTotals = groupByDay(previous);

        BigDecimal prevAvgPerDay = prevDailyTotals.isEmpty()
                ? BigDecimal.ZERO
                : prevTotal.divide(BigDecimal.valueOf(prevDailyTotals.size()), RoundingMode.HALF_UP);

        BigDecimal delta = prevTotal.compareTo(BigDecimal.ZERO) > 0
                ? total.subtract(prevTotal).multiply(BigDecimal.valueOf(100)).divide(prevTotal, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return ReportSummary.builder()
                .totalAmount(total)
                .averagePerDay(averagePerDay)
                .topCategory(topCategory)
                .topDay(topDay)
                .topTransactions(top5Transactions)
                .activeDaysCount(activeDays)
                .uniqueCategoriesCount((int) uniqueCategories)
                .previousTotalAmount(prevTotal)
                .previousAveragePerDay(prevAvgPerDay)
                .deltaPercentage(delta)
                .build();
    }

    public BigDecimal getTotalForPeriod(List<TransactionResponse> transactions, LocalDate start, LocalDate end) {
        return transactions.stream()
                .filter(t -> {
                    LocalDate date = LocalDate.parse(t.getDate().substring(0, 10));
                    return (!date.isBefore(start)) && (!date.isAfter(end));
                })
                .map(TransactionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<LocalDate, BigDecimal> groupByDay(List<TransactionResponse> txs) {
        return txs.stream()
                .collect(Collectors.groupingBy(
                        t -> LocalDate.parse(t.getDate().substring(0, 10)),
                        Collectors.reducing(BigDecimal.ZERO, TransactionResponse::getAmount, BigDecimal::add)
                ));
    }

    private BigDecimal sum(List<TransactionResponse> txs) {
        return txs.stream()
                .map(TransactionResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
