package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.model.dto.TransactionStatsResponse;
import com.miscroservice.transaction_service.model.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class TransactionStatsTest extends BaseTransactionTest{
    @Test
    void getStats_Success() {
        // Arrange
        when(transactionRepository.findByUserId(userId)).thenReturn(List.of(transaction));

        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // Кэш пуст
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any());

        // Act
        TransactionStatsResponse stats = transactionService.getStats(userId, null, null);

        // Assert
        assertNotNull(stats);
        assertEquals(new BigDecimal("100.00"), stats.getTotalIncome());
        assertEquals(BigDecimal.ZERO, stats.getTotalExpense());
        assertEquals(1, stats.getByCategory().size());
        assertEquals(new BigDecimal("100.00"), stats.getByCategory().get("Salary"));
        verify(transactionRepository).findByUserId(userId);
        verify(redisTemplate, times(2)).opsForValue(); // Ожидаем два вызова opsForValue
        verify(valueOperations).get(anyString());
        verify(valueOperations).set(anyString(), any(), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getStats_FromCache_Success() {
        // Arrange
        TransactionStatsResponse cachedStats = new TransactionStatsResponse();
        cachedStats.setTotalIncome(new BigDecimal("100.00"));
        cachedStats.setTotalExpense(BigDecimal.ZERO);
        Map<String, BigDecimal> byCategory = new HashMap<>();
        byCategory.put("Salary", new BigDecimal("100.00"));
        cachedStats.setByCategory(byCategory);

        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(cachedStats); // Данные есть в кэше

        // Act
        TransactionStatsResponse stats = transactionService.getStats(userId, null, null);

        // Assert
        assertNotNull(stats);
        assertEquals(new BigDecimal("100.00"), stats.getTotalIncome());
        assertEquals(BigDecimal.ZERO, stats.getTotalExpense());
        assertEquals(1, stats.getByCategory().size());
        assertEquals(new BigDecimal("100.00"), stats.getByCategory().get("Salary"));
        verify(transactionRepository, never()).findByUserId(any()); // Репозиторий не вызывается
        verify(redisTemplate, times(1)).opsForValue(); // Только один вызов opsForValue
        verify(valueOperations).get(anyString());
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void getStats_WithDateFilter_Success() {
        // Arrange
        Instant now = Instant.now();
        Transaction oldTransaction = new Transaction();
        oldTransaction.setId(UUID.randomUUID());
        oldTransaction.setUserId(userId);
        oldTransaction.setAmount(new BigDecimal("50.00"));
        oldTransaction.setType("EXPENSE");
        oldTransaction.setCategory("Groceries");
        oldTransaction.setDate(now.minusSeconds(3600)); // 1 час назад
        oldTransaction.setCreatedAt(now);
        oldTransaction.setUpdatedAt(now);

        when(transactionRepository.findByUserId(userId)).thenReturn(List.of(transaction, oldTransaction));

        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any());

        String startDate = now.minusSeconds(1800).toString(); // 30 минут назад
        String endDate = now.plusSeconds(1800).toString(); // 30 минут вперед

        // Act
        TransactionStatsResponse stats = transactionService.getStats(userId, startDate, endDate);

        // Assert
        assertNotNull(stats);
        assertEquals(new BigDecimal("100.00"), stats.getTotalIncome()); // Только текущая транзакция попадает в диапазон
        assertEquals(BigDecimal.ZERO, stats.getTotalExpense()); // oldTransaction вне диапазона
        assertEquals(1, stats.getByCategory().size());
        assertEquals(new BigDecimal("100.00"), stats.getByCategory().get("Salary"));
        verify(transactionRepository).findByUserId(userId);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(anyString());
        verify(valueOperations).set(anyString(), any(), eq(10L), eq(TimeUnit.MINUTES));
    }
}
