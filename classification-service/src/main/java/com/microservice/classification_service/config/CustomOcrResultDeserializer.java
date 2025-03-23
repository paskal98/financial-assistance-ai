package com.microservice.classification_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microservice.classification_service.model.dto.OcrResultMessage;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CustomOcrResultDeserializer implements Deserializer<OcrResultMessage> {
    private static final Logger logger = LoggerFactory.getLogger(CustomOcrResultDeserializer.class);
    private final ObjectMapper objectMapper;

    public CustomOcrResultDeserializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public OcrResultMessage deserialize(String topic, byte[] data) {
        if (data == null) {
            logger.warn("Received null data for topic: {}", topic);
            return null;
        }
        try {
            return objectMapper.readValue(data, OcrResultMessage.class);
        } catch (Exception e) {
            logger.error("Failed to deserialize message from topic {}: {}", topic, new String(data), e);
            throw new SerializationException("Error deserializing OcrResultMessage", e);
        }
    }

    @Override
    public void close() {}
}