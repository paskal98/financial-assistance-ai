package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

public class TransactionFetchingTest extends BaseTransactionTest{

    @Test
    void getTransactions_FromCache_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<TransactionResponse> cachedPage = new PageImpl<>(List.of(
                new TransactionResponse(transaction.getId(), transaction.getAmount(), transaction.getType(),
                        transaction.getCategory(), transaction.getDescription(), transaction.getDate().toString())
        ));

        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(cachedPage); // Данные есть в кэше

        // Act
        Page<TransactionResponse> result = transactionService.getTransactions(userId, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        TransactionResponse response = result.getContent().get(0);
        assertEquals(transaction.getId(), response.getId());
        verify(transactionRepository, never()).findByFilters(any(), any(), any(), any(), any(), any()); // Репозиторий не вызывается
        verify(redisTemplate, times(1)).opsForValue(); // Только один вызов opsForValue
        verify(valueOperations).get(anyString());
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    void getTransactions_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findByFilters(userId, null, null, null, null, pageable))
                .thenReturn(transactionPage);

        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null); // Кэш пуст
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any()); // Мокаем set

        // Act
        Page<TransactionResponse> result = transactionService.getTransactions(userId, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        TransactionResponse response = result.getContent().get(0);
        assertEquals(transaction.getId(), response.getId());
        verify(transactionRepository).findByFilters(userId, null, null, null, null, pageable);
        verify(redisTemplate, times(2)).opsForValue(); // Ожидаем два вызова opsForValue
        verify(valueOperations).get(anyString()); // Проверяем попытку чтения из кэша
        verify(valueOperations).set(anyString(), any(), eq(10L), eq(TimeUnit.MINUTES)); // Проверяем запись в кэш
    }

    @Test
    void getTransactions_EmptyResult() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> emptyPage = new PageImpl<>(Collections.emptyList());
        when(transactionRepository.findByFilters(userId, null, null, null, null, pageable)).thenReturn(emptyPage);

        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any());

        // Act
        Page<TransactionResponse> result = transactionService.getTransactions(userId, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(transactionRepository).findByFilters(userId, null, null, null, null, pageable);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(anyString());
        verify(valueOperations).set(anyString(), any(), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getTransactions_WithFilters_Success() {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofDays(1));
        Instant to = now.plus(Duration.ofDays(1));
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction));
        when(transactionRepository.findByFilters(
                eq(userId),
                eq(from),
                eq(to),
                eq("Salary"),
                eq("INCOME"),
                eq(pageable)
        )).thenReturn(transactionPage);

        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(valueOperations).set(anyString(), any(), anyLong(), any());

        // Act
        Page<TransactionResponse> result = transactionService.getTransactions(
                userId,
                from,
                to,
                "Salary",
                "INCOME",
                pageable
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        TransactionResponse response = result.getContent().get(0);
        assertEquals(transaction.getId(), response.getId());
        verify(transactionRepository).findByFilters(userId, from, to, "Salary", "INCOME", pageable);
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOperations).get(anyString());
        verify(valueOperations).set(anyString(), any(), eq(10L), eq(TimeUnit.MINUTES));
    }
}
