package com.microservice.document_processing_service.service.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ProcessingStateServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    private ProcessingStateService processingStateService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        processingStateService = new ProcessingStateService(redisTemplate);
    }

    @Test
    void initializeState_Success_SetsRedisState() throws Exception {
        UUID documentId = UUID.randomUUID();
        String key = "document:processing:" + documentId;

        processingStateService.initializeState(documentId, 3);

        verify(valueOps).set(eq(key), argThat(json -> json.contains("\"totalItems\":3")));
    }

    @Test
    void incrementProcessed_RedisFailure_UsesFallback() throws Exception {
        UUID documentId = UUID.fromString("69a05f5c-2086-4d9a-a1d9-e991e81c0aa9");
        int totalItems = 2;

        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionException("Redis down"));
        processingStateService.initializeState(documentId, totalItems);

        doThrow(new RedisConnectionException("Redis down")).when(redisTemplate).opsForValue();

        processingStateService.incrementProcessed(documentId);
        processingStateService.incrementProcessed(documentId);

        assertTrue(processingStateService.isProcessingComplete(documentId));
    }


    @Test
    void isProcessingComplete_Complete_ClearsState() throws Exception {
        UUID documentId = UUID.randomUUID();
        String key = "document:processing:" + documentId;
        String stateJson = objectMapper.writeValueAsString(Map.of("totalItems", 1, "processedItems", 1));
        when(valueOps.get(key)).thenReturn(stateJson);

        boolean complete = processingStateService.isProcessingComplete(documentId);

        assertTrue(complete);
        verify(redisTemplate).delete(key);
    }
}