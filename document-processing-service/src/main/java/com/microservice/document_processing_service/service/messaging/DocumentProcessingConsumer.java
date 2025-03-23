package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.repository.DocumentRepository;
import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import com.microservice.document_processing_service.service.processing.ProcessingStateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    private final DocumentStateManager documentStateManager;
    private final ProcessingStateService processingStateService;
    private final DocumentRepository documentRepository;

    @KafkaListener(topics = "document-transaction-feedback", groupId = "doc-feedback-group",
            containerFactory = "feedbackKafkaListenerContainerFactory")
    public void handleTransactionFeedback(String feedbackMessage) {
        try {
            String[] parts = feedbackMessage.split("\\|");
            UUID documentId = UUID.fromString(parts[0]);
            String itemName = parts[1];
            String status = parts[2];
            String errorMessage = parts.length > 3 ? parts[3] : null;

            if ("SUCCESS".equals(status)) {
                processingStateService.incrementProcessed(documentId);
                if (processingStateService.isProcessingComplete(documentId)) {
                    documentStateManager.updateStatus(documentId, "PROCESSED");
                }
            } else if ("FAILED".equals(status)) {
                documentStateManager.updateStatus(documentId, "FAILED",
                        String.format("Transaction '%s' failed: %s", itemName, errorMessage));
                processingStateService.clearState(documentId);
            }
            logger.info("Processed feedback for document {}: status={}, item={}", documentId, status, itemName);
        } catch (Exception e) {
            logger.error("Failed to process feedback: {}", feedbackMessage, e);
        }
    }
}
