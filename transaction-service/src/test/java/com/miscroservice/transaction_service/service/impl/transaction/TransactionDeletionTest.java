package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.exception.AccessDeniedException;
import com.miscroservice.transaction_service.exception.TransactionNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransactionDeletionTest extends BaseTransactionTest{

    @Test
    void deleteTransaction_Success() {
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("mockedKey"));

        transactionService.deleteTransaction(transaction.getId(), userId);

        verify(transactionRepository).findById(transaction.getId());
        verify(transactionRepository).delete(transaction);
        verify(redisTemplate).keys(TRANSACTIONS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).keys(STATS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).delete(anyCollection());
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
