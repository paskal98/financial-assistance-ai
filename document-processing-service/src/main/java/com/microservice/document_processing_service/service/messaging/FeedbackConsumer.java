package com.microservice.document_processing_service.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import com.microservice.document_processing_service.service.processing.ProcessingStateService;

import lombok.RequiredArgsConstructor;
import org.shared.dto.FeedbackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackConsumer {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackConsumer.class);

    private final DocumentStateManager documentStateManager;
    private final ProcessingStateService processingStateService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "document-feedback-queue", groupId = "doc-feedback-group",
            containerFactory = "feedbackKafkaListenerContainerFactory")
    public void handleFeedback(String feedbackJson) {
        try {
            FeedbackMessage feedback = objectMapper.readValue(feedbackJson, FeedbackMessage.class);
            UUID documentId = UUID.fromString(feedback.getDocumentId());
            String stage = feedback.getStage();
            String status = feedback.getStatus();
            String details = feedback.getDetails();

            logger.info("Received feedback for document {}: stage={}, status={}, details={}", documentId, stage, status, details);

            switch (stage) {
                case "OCR":
                    documentStateManager.updateStatus(documentId, status, details);
                    break;
                case "CLASSIFICATION":
                    if ("ITEMS_COUNT".equals(status)) {
                        int totalItems = Integer.parseInt(details);
                        processingStateService.initializeState(documentId, totalItems);
                        documentStateManager.updateStatus(documentId, "CLASSIFYING");
                    } else if ("FAILED".equals(status)) {
                        documentStateManager.updateStatus(documentId, "FAILED", "Classification error: " + details);
                        processingStateService.clearState(documentId);
                    } else {
                        documentStateManager.updateStatus(documentId, status, details);
                    }
                    break;
                case "TRANSACTION":
                    if ("SUCCESS".equals(status)) {
                        processingStateService.incrementProcessed(documentId);
                        if (processingStateService.isProcessingComplete(documentId)) {
                            documentStateManager.updateStatus(documentId, "PROCESSED");
                        }
                    } else if ("FAILED".equals(status)) {
                        documentStateManager.updateStatus(documentId, "FAILED", "Transaction error: " + details);
                        processingStateService.clearState(documentId);
                    }
                    break;
                default:
                    logger.warn("Unknown stage '{}' for document {}", stage, documentId);
            }
        } catch (Exception e) {
            logger.error("Failed to process feedback: {}", feedbackJson, e);
        }
    }
}
