package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.exception.ValidationException;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.entity.Category;
import com.miscroservice.transaction_service.model.entity.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.validation.FieldError;

import java.time.format.DateTimeParseException;
import java.util.List;
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

        TransactionResponse response = transactionService.createTransaction(transactionRequest, userId, bindingResult);

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
                () -> transactionService.createTransaction(transactionRequest, userId, bindingResult));
        assertEquals("Category 'InvalidCategory' does not exist", exception.getMessage());
        verify(categoryRepository).findByName("InvalidCategory");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_InvalidType_ThrowsIllegalArgumentException() {
        when(categoryRepository.findByName("Salary")).thenReturn(Optional.of(new Category()));
        transactionRequest.setType("INVALID_TYPE");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(transactionRequest, userId, bindingResult));
        assertEquals("Type must be either 'INCOME' or 'EXPENSE'", exception.getMessage());
        verify(categoryRepository).findByName("Salary");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_InvalidDateFormat_ThrowsException() {
        when(categoryRepository.findByName("Salary")).thenReturn(Optional.of(new Category()));
        transactionRequest.setDate("invalid-date-format"); // Некорректный формат

        assertThrows(DateTimeParseException.class,
                () -> transactionService.createTransaction(transactionRequest, userId, bindingResult));
        verify(categoryRepository).findByName("Salary");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_MissingRequiredFields_ThrowsValidationException() {
        // Настраиваем BindingResult с ошибками
        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("transactionRequest", "amount", "Amount cannot be null"),
                new FieldError("transactionRequest", "type", "Type cannot be blank")
        ));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> transactionService.createTransaction(transactionRequest, userId, bindingResult));
        assertTrue(exception.getMessage().contains("amount: Amount cannot be null"));
        assertTrue(exception.getMessage().contains("type: Type cannot be blank"));
        verify(transactionRepository, never()).save(any());
        verify(categoryRepository, never()).findByName(anyString());
    }
}