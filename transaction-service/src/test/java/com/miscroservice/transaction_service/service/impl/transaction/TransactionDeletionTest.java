package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.exception.AccessDeniedException;
import com.miscroservice.transaction_service.exception.TransactionNotFoundException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.support.SendResult;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransactionDeletionTest extends BaseTransactionTest{

    @Test
    void deleteTransaction_Success() {
        // Arrange
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("mockedKey"));

        // Mock redisTemplate.opsForValue()
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null); // Event not processed yet
        doNothing().when(valueOps).set(anyString(), any(), anyLong(), any());

        // Mock KafkaTemplate.send to return a CompletableFuture
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(balanceKafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // Act
        transactionService.deleteTransaction(transaction.getId(), userId);

        // Assert
        verify(transactionRepository).findById(transaction.getId());
        verify(transactionRepository).delete(transaction);
        verify(balanceKafkaTemplate).send(any(ProducerRecord.class));
        verify(redisTemplate).keys(TRANSACTIONS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).keys(STATS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).delete(anyCollection());
        verify(valueOps).get(anyString());
        verify(valueOps).set(anyString(), eq("true"), anyLong(), any());
    }

    @Test
    void deleteTransaction_NotFound_ThrowsException() {
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.deleteTransaction(transaction.getId(), userId));
        verify(transactionRepository).findById(transaction.getId());
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void deleteTransaction_AccessDenied_ThrowsException() {
        // Arrange
        UUID differentUserId = UUID.randomUUID();
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> transactionService.deleteTransaction(transaction.getId(), differentUserId));
        verify(transactionRepository).findById(transaction.getId());
        verify(transactionRepository, never()).delete(any());
    }
}
