package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.entity.Category;
import com.miscroservice.transaction_service.model.entity.Transaction;
import org.junit.jupiter.api.Test;

import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TransactionCreationTest extends BaseTransactionTest {

    @Test
    void createTransaction_Success() {
        when(categoryRepository.findByName("Salary")).thenReturn(Optional.of(new Category()));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("mockedKey"));

        TransactionResponse response = transactionService.createTransaction(transactionRequest, userId);

        assertNotNull(response);
        assertEquals(transaction.getId(), response.getId());
        assertEquals(transaction.getAmount(), response.getAmount());
        verify(categoryRepository).findByName("Salary");
        verify(transactionRepository).save(any(Transaction.class));
        verify(redisTemplate).keys(TRANSACTIONS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).keys(STATS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).delete(anyCollection());
    }

    @Test
    void createTransaction_InvalidCategory_ThrowsIllegalArgumentException() {
        when(categoryRepository.findByName("InvalidCategory")).thenReturn(Optional.empty());
        transactionRequest.setCategory("InvalidCategory");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(transactionRequest, userId));
        assertEquals("Category 'InvalidCategory' does not exist", exception.getMessage());
        verify(categoryRepository).findByName("InvalidCategory");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_InvalidType_ThrowsIllegalArgumentException() {
        // Arrange
        when(categoryRepository.findByName("Salary")).thenReturn(Optional.of(new Category()));
        transactionRequest.setType("INVALID_TYPE");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(transactionRequest, userId));
        assertEquals("Type must be either 'INCOME' or 'EXPENSE'", exception.getMessage());
        verify(categoryRepository).findByName("Salary");
        verify(transactionRepository, never()).save(any());
    }


    @Test
    void createTransaction_InvalidDateFormat_ThrowsException() {
        // Arrange
        when(categoryRepository.findByName("Salary")).thenReturn(Optional.of(new Category()));
        transactionRequest.setDate("invalid-date-format"); // Некорректный формат

        // Act & Assert
        assertThrows(DateTimeParseException.class,
                () -> transactionService.createTransaction(transactionRequest, userId));
        verify(categoryRepository).findByName("Salary");
        verify(transactionRepository, never()).save(any());
    }

}
