package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.model.dto.TransactionItemDto;
import com.miscroservice.transaction_service.model.entity.Category;
import com.miscroservice.transaction_service.model.entity.Transaction;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.shared.utils.KafkaUtils;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TransactionDocumentProcessingTest extends BaseTransactionTest{

    @Test
    void processTransactionFromDocument_Success() {
        // Arrange
        TransactionItemDto item = new TransactionItemDto();
        item.setName("Test Transaction");
        item.setCategory("Salary");
        item.setType("INCOME");
        item.setPrice(new BigDecimal("200.00"));
        item.setDate(Instant.now());
        item.setPaymentMethod("Card");
        UUID documentId = UUID.randomUUID();

        when(categoryRepository.findByName("Salary")).thenReturn(Optional.of(new Category()));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(redisTemplate.keys(anyString())).thenReturn(Set.of("mockedKey"));

        // Mock redisTemplate.opsForValue()
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null); // Event not processed yet
        doNothing().when(valueOps).set(anyString(), any(), anyLong(), any());

        // Mock KafkaTemplate.send for both feedback and balance updates
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(feedbackKafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);
        when(balanceKafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // Act
        transactionService.processTransactionFromDocument(item, userId, documentId);

        // Assert
        verify(categoryRepository).findByName("Salary");
        verify(transactionRepository).save(any(Transaction.class));

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(feedbackKafkaTemplate).send(captor.capture());

        ProducerRecord<String, String> sentRecord = captor.getValue();
        assertEquals("document-feedback-queue", sentRecord.topic());
        assertTrue(sentRecord.value().contains("SUCCESS"));
        assertTrue(sentRecord.value().contains("Test Transaction"));

        verify(balanceKafkaTemplate).send(any(ProducerRecord.class));
        verify(redisTemplate).keys(TRANSACTIONS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).keys(STATS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).delete(anyCollection());
        verify(valueOps).get(anyString());
        verify(valueOps).set(anyString(), eq("true"), anyLong(), any());
    }


    @Test
    void processTransactionFromDocument_InvalidCategory_ThrowsException() {
        // Arrange
        TransactionItemDto item = new TransactionItemDto();
        item.setName("Test Transaction");
        item.setCategory("InvalidCategory");
        item.setType("INCOME");
        item.setPrice(new BigDecimal("200.00"));
        item.setDate(Instant.now());
        item.setUserId(userId);
        item.setDocumentId(UUID.randomUUID());

        // Статический мок KafkaUtils
        try (MockedStatic<KafkaUtils> mockedKafkaUtils = mockStatic(KafkaUtils.class)) {
            mockedKafkaUtils.when(() -> KafkaUtils.sendFeedback(any(), anyString(), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            when(categoryRepository.findByName("InvalidCategory")).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    transactionService.processTransactionFromDocument(item, userId, item.getDocumentId())
            );

            assertEquals("Category 'InvalidCategory' does not exist", ex.getMessage());

            verify(categoryRepository).findByName("InvalidCategory");

            mockedKafkaUtils.verify(() ->
                    KafkaUtils.sendFeedback(any(), eq("document-feedback-queue"),
                            argThat(msg -> msg.getStatus().equals("FAILED") &&
                                    msg.getDetails().contains("InvalidCategory")))
            );

            verify(transactionRepository, never()).save(any());
            verify(redisTemplate, never()).keys(anyString());
            verify(redisTemplate, never()).delete(anyCollection());
        }
    }
}
