package com.microservice.document_processing_service.service.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisConnectionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ProcessingStateService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingStateService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${processing.state.ttl:24}") // Часы, по умолчанию 24 часа
    private long ttlHours;

    // Fallback хранилище в памяти
    private final Map<UUID, Map<String, Integer>> fallbackState = new ConcurrentHashMap<>();

    private String getKey(UUID documentId) {
        return "document:processing:" + documentId;
    }

    @Retryable(value = RedisConnectionException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void initializeState(UUID documentId, int totalItems) {
        Map<String, Integer> state = new HashMap<>();
        state.put("totalItems", totalItems);
        state.put("processedItems", 0);

        try {
            String key = getKey(documentId);
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(state));
            redisTemplate.expire(key, Duration.ofHours(ttlHours));
            logger.info("Initialized processing state for document {} with {} items, TTL set to {} hours", documentId, totalItems, ttlHours);
        } catch (Exception e) {
            logger.error("Failed to initialize state in Redis for document {}: {}. Switching to fallback.", documentId, e.getMessage(), e);
            fallbackState.put(documentId, state); // Переключаемся на локальное хранилище
        }
    }

    public void incrementProcessed(UUID documentId) {
        String key = getKey(documentId);
        try {
            String stateJson = redisTemplate.opsForValue().get(key);
            if (stateJson == null) {
                logger.warn("No processing state found in Redis for document: {}. Checking fallback.", documentId);
                incrementFallbackProcessed(documentId);
                return;
            }
            Map<String, Integer> state = objectMapper.readValue(stateJson, HashMap.class);
            state.put("processedItems", state.get("processedItems") + 1);
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(state));
            logger.debug("Incremented processed items for document {}: {}", documentId, state.get("processedItems"));
        } catch (Exception e) {
            logger.error("Failed to increment processed items in Redis for document {}: {}. Switching to fallback.", documentId, e.getMessage(), e);
            incrementFallbackProcessed(documentId);
        }
    }

    public boolean isProcessingComplete(UUID documentId) {
        String key = getKey(documentId);
        try {
            String stateJson = redisTemplate.opsForValue().get(key);
            if (stateJson == null) {
                logger.warn("No processing state found in Redis for document: {}. Checking fallback.", documentId);
                return isFallbackProcessingComplete(documentId);
            }
            Map<String, Integer> state = objectMapper.readValue(stateJson, HashMap.class);
            boolean complete = state.get("processedItems") >= state.get("totalItems");
            if (complete) {
                redisTemplate.delete(key);
                logger.info("Processing complete for document {}, state cleared from Redis", documentId);
            }
            return complete;
        } catch (Exception e) {
            logger.error("Failed to check processing state in Redis for document {}: {}. Switching to fallback.", documentId, e.getMessage(), e);
            return isFallbackProcessingComplete(documentId);
        }
    }

    public void clearState(UUID documentId) {
        try {
            redisTemplate.delete(getKey(documentId));
            logger.info("Cleared processing state for document {} from Redis", documentId);
        } catch (Exception e) {
            logger.error("Failed to clear state in Redis for document {}: {}. Clearing from fallback.", documentId, e.getMessage(), e);
        }
        fallbackState.remove(documentId); // Очистка и из fallback
    }

    // Fallback-методы для работы с локальным хранилищем
    private void incrementFallbackProcessed(UUID documentId) {
        Map<String, Integer> state = fallbackState.get(documentId);
        if (state == null) {
            logger.warn("No processing state found in fallback for document: {}", documentId);
            return;
        }
        state.put("processedItems", state.get("processedItems") + 1);
        logger.debug("Incremented processed items in fallback for document {}: {}", documentId, state.get("processedItems"));
    }

    private boolean isFallbackProcessingComplete(UUID documentId) {
        Map<String, Integer> state = fallbackState.get(documentId);
        if (state == null) {
            logger.warn("No processing state found in fallback for document: {}", documentId);
            return false;
        }
        boolean complete = state.get("processedItems") >= state.get("totalItems");
        if (complete) {
            fallbackState.remove(documentId);
            logger.info("Processing complete for document {}, state cleared from fallback", documentId);
        }
        return complete;
    }

    @Recover
    public void recoverInitializeState(RedisConnectionException e, UUID documentId, int totalItems) {
        logger.error("All retries failed for Redis. Switching to fallback for document {}", documentId, e);
        Map<String, Integer> state = new HashMap<>();
        state.put("totalItems", totalItems);
        state.put("processedItems", 0);
        fallbackState.put(documentId, state);
    }
}