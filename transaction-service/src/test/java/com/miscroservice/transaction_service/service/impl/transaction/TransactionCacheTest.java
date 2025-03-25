package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

public class TransactionCacheTest extends BaseTransactionTest {

    @Test
    void invalidateCache_NoKeysToDelete() {
        // Arrange
        when(redisTemplate.keys(TRANSACTIONS_CACHE_PREFIX + userId + "*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys(STATS_CACHE_PREFIX + userId + "*")).thenReturn(Collections.emptySet());

        // Используем рефлексию для вызова private метода или создаем вспомогательный метод
        try {
            Method invalidateCacheMethod = TransactionServiceImpl.class.getDeclaredMethod("invalidateCache", UUID.class);
            invalidateCacheMethod.setAccessible(true);
            invalidateCacheMethod.invoke(transactionService, userId);
        } catch (Exception e) {
            fail("Failed to invoke invalidateCache: " + e.getMessage());
        }

        // Assert
        verify(redisTemplate).keys(TRANSACTIONS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).keys(STATS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    void invalidateCache_RedisFailure_ContinuesExecution() throws Exception {
        // Arrange
        when(redisTemplate.keys(TRANSACTIONS_CACHE_PREFIX + userId + "*"))
                .thenThrow(new RuntimeException("Redis unavailable"));
        when(redisTemplate.keys(STATS_CACHE_PREFIX + userId + "*"))
                .thenReturn(Collections.emptySet());

        // Act
        Method method = TransactionServiceImpl.class.getDeclaredMethod("invalidateCache", UUID.class);
        method.setAccessible(true);
        method.invoke(transactionService, userId);

        // Assert
        verify(redisTemplate).keys(TRANSACTIONS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).keys(STATS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    void invalidateCache_RedisFailureForBoth_ContinuesExecution() throws Exception {
        // Arrange
        when(redisTemplate.keys(anyString())).thenThrow(new RuntimeException("Redis unavailable"));

        // Act
        Method method = TransactionServiceImpl.class.getDeclaredMethod("invalidateCache", UUID.class);
        method.setAccessible(true);
        method.invoke(transactionService, userId);

        // Assert
        verify(redisTemplate).keys(TRANSACTIONS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate).keys(STATS_CACHE_PREFIX + userId + "*");
        verify(redisTemplate, never()).delete(anyCollection());
    }

}
