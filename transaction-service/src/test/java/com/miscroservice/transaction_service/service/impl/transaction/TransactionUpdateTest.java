package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.exception.AccessDeniedException;
import com.miscroservice.transaction_service.exception.TransactionNotFoundException;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.entity.Category;
import com.miscroservice.transaction_service.model.entity.Transaction;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TransactionUpdateTest extends BaseTransactionTest{
    @Test
    void updateTransaction_Success() {
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(categoryRepository.findByName("Salary")).thenReturn(Optional.of(new Category()));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("mockedKey"));

        TransactionResponse response = transactionService.updateTransaction(transaction.getId(), transactionRequest, userId);

        assertNotNull(response);
        assertEquals(transaction.getId(), response.getId());
        verify(transactionRepository).findById(transaction.getId());
        verify(transactionRepository).save(any(Transaction.class));
        verify(redisTemplate).keys(TRANSACTIONS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).keys(STATS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).delete(anyCollection());
    }

    @Test
    void updateTransaction_NotFound_ThrowsException() {
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> transactionService.updateTransaction(transaction.getId(), transactionRequest, userId));
        verify(transactionRepository).findById(transaction.getId());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_AccessDenied_ThrowsException() {
        UUID differentUserId = UUID.randomUUID();
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThrows(AccessDeniedException.class,
                () -> transactionService.updateTransaction(transaction.getId(), transactionRequest, differentUserId));
        verify(transactionRepository).findById(transaction.getId());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_InvalidCategory_ThrowsIllegalArgumentException() {
        // Arrange
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(categoryRepository.findByName("InvalidCategory")).thenReturn(Optional.empty());
        transactionRequest.setCategory("InvalidCategory");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transactionService.updateTransaction(transaction.getId(), transactionRequest, userId));
        assertEquals("Category 'InvalidCategory' does not exist", exception.getMessage());
        verify(transactionRepository).findById(transaction.getId());
        verify(categoryRepository).findByName("InvalidCategory");
        verify(transactionRepository, never()).save(any());
    }
}
