package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.exception.AccessDeniedException;
import com.miscroservice.transaction_service.exception.TransactionNotFoundException;
import com.miscroservice.transaction_service.exception.ValidationException;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.entity.Category;
import com.miscroservice.transaction_service.model.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TransactionUpdateTest extends BaseTransactionTest {

    @Test
    void updateTransaction_Success() {
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(categoryRepository.findByName("Salary")).thenReturn(Optional.of(new Category()));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("mockedKey"));

        TransactionResponse response = transactionService.updateTransaction(transaction.getId(), transactionRequest, userId, bindingResult);

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
                () -> transactionService.updateTransaction(transaction.getId(), transactionRequest, userId, bindingResult));
        verify(transactionRepository).findById(transaction.getId());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_AccessDenied_ThrowsException() {
        UUID differentUserId = UUID.randomUUID();
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThrows(AccessDeniedException.class,
                () -> transactionService.updateTransaction(transaction.getId(), transactionRequest, differentUserId, bindingResult));
        verify(transactionRepository).findById(transaction.getId());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_InvalidCategory_ThrowsIllegalArgumentException() {
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(categoryRepository.findByName("InvalidCategory")).thenReturn(Optional.empty());
        transactionRequest.setCategory("InvalidCategory");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transactionService.updateTransaction(transaction.getId(), transactionRequest, userId, bindingResult));
        assertEquals("Category 'InvalidCategory' does not exist", exception.getMessage());
        verify(transactionRepository).findById(transaction.getId());
        verify(categoryRepository).findByName("InvalidCategory");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransaction_MissingRequiredFields_ThrowsValidationException() {
        // Setup: Mock binding result with errors
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("transactionRequest", "amount", "Amount cannot be null"),
                new FieldError("transactionRequest", "category", "Category cannot be blank")
        ));

        // Act: Expect ValidationException due to invalid input
        ValidationException exception = assertThrows(ValidationException.class,
                () -> transactionService.updateTransaction(transaction.getId(), transactionRequest, userId, bindingResult));

        // Assert: Check exception message
        assertTrue(exception.getMessage().contains("amount: Amount cannot be null"));
        assertTrue(exception.getMessage().contains("category: Category cannot be blank"));

        // Verify: No repository interactions should occur
        verify(transactionRepository, never()).findById(any());
        verify(transactionRepository, never()).save(any());
        verify(categoryRepository, never()).findByName(anyString());
    }
}