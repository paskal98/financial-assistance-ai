package com.microservice.classification_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microservice.classification_service.exception.DocumentProcessingException;
import com.microservice.classification_service.model.dto.TransactionItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class ResponseParser {
    private static final Logger logger = LoggerFactory.getLogger(ResponseParser.class);
    private final ObjectMapper objectMapper;

    public ResponseParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public List<TransactionItemDto> parseResponse(String aiResponse, UUID documentId) {
        try {
            int start = aiResponse.indexOf('[');
            int end = aiResponse.lastIndexOf(']');

            if (start == -1 || end == -1) {
                throw new Exception("Invalid JSON format in OpenAI response for document: " + documentId);
            }

            aiResponse = aiResponse.substring(start, end + 1);
            List<TransactionItemDto> items = objectMapper.readValue(aiResponse, new TypeReference<>() {});

            Instant now = Instant.now();
            Instant oneMonthAgo = now.minus(30, ChronoUnit.DAYS);

            for (TransactionItemDto item : items) {
                if (Objects.equals(item.getName(), "NONSENSE")) {
                    throw new Exception("Unrecognizable receipt content for document: " + documentId);
                }
                if (item.getName() == null || item.getName().trim().isEmpty()) {
                    item.setName("Unknown Item");
                }
                if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                    item.setCategory("Unknown");
                }
                if (item.getType() == null || item.getType().trim().isEmpty()) {
                    item.setType("EXPENSE");
                }
                if (item.getPrice() == null) {
                    item.setPrice(BigDecimal.ZERO);
                }
                if (item.getDate() == null) {
                    item.setDate(now);
                }
                if (item.getDate().isAfter(now) || item.getDate().isBefore(oneMonthAgo)) {
                    logger.debug("Adjusting date for item {} from {} to now for document {}", item.getName(), item.getDate(), documentId);
                    item.setDate(now);
                }
                if (item.getDescription() == null) {
                    item.setDescription("No description");
                }
                if (item.getPaymentMethod() == null) {
                    item.setPaymentMethod("Unknown");
                }
                if (item.getDocumentId() == null) {
                    item.setDocumentId(documentId);
                }
            }

            logger.info("Parsed {} items from AI response for document {}", items.size(), documentId);
            return items;
        } catch (Exception e) {
            logger.error("Failed to parse AI response for document {}: {}", documentId, aiResponse, e);
            throw new DocumentProcessingException("Invalid AI response format for document: " + documentId, e);
        }
    }
}
