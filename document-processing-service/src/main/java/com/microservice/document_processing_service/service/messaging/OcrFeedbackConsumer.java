package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OcrFeedbackConsumer {
    private static final Logger logger = LoggerFactory.getLogger(OcrFeedbackConsumer.class);

    private final DocumentStateManager documentStateManager;

    @KafkaListener(topics = "ocr-feedback-queue", groupId = "ocr-feedback-group",
            containerFactory = "ocrFeedbackKafkaListenerContainerFactory")
    public void handleOcrFeedback(String feedbackMessage) {
        try {
            String[] parts = feedbackMessage.split("\\|");
            UUID documentId = UUID.fromString(parts[0]);
            String status = parts[1]; // Например, "FAILED"
            String errorMessage = parts.length > 2 ? parts[2] : null;

            documentStateManager.updateStatus(documentId, status, errorMessage);
            logger.info("Processed OCR feedback for document {}: status={}, error={}", documentId, status, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to process OCR feedback: {}", feedbackMessage, e);
        }
    }
}