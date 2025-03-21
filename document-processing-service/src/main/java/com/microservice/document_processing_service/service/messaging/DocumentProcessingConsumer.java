package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import com.microservice.document_processing_service.model.entity.Document;
import com.microservice.document_processing_service.repository.DocumentRepository;
import com.microservice.document_processing_service.service.TransactionProducerService;
import com.microservice.document_processing_service.service.processing.DocumentProcessor;
import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import com.microservice.document_processing_service.service.processing.ProcessingStateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    private final DocumentProcessor documentProcessor;
    private final DocumentStateManager documentStateManager;
    private final TransactionProducerService transactionProducerService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DocumentRepository documentRepository;
    private final ProcessingStateService processingStateService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            exclude = {IllegalArgumentException.class}
    )
    @KafkaListener(topics = "document-processing-queue", groupId = "doc-processing-group")
    public void processDocument(String message) {
        String[] parts = message.split("\\|", 4);
        String filePath = parts[0];
        UUID userId = UUID.fromString(parts[1]);
        UUID documentId = UUID.fromString(parts[2]);
        String date = parts.length > 3 ? parts[3] : null;

        try {
            logger.info("Received message for document processing: {}", message);

            documentStateManager.updateStatus(documentId, "PROCESSING");
            List<TransactionItemDto> items = documentProcessor.processDocument(filePath, documentId, userId, date);

            processingStateService.initializeState(documentId, items.size());
            items.forEach(item -> transactionProducerService.sendTransaction(item, userId, documentId));

            logger.info("Document sent for transaction processing: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to process document: {}", message, e);
            String errorMessage = determineErrorMessage(e);
            documentStateManager.updateStatus(documentId, "FAILED", errorMessage);
            processingStateService.clearState(documentId); // Очистка состояния при ошибке
            sendToDlq(message, e.getMessage());
        }
    }

    @KafkaListener(topics = "document-transaction-feedback", groupId = "doc-feedback-group")
    public void handleTransactionFeedback(String feedbackMessage) {
        String[] parts = feedbackMessage.split("\\|");
        UUID documentId = UUID.fromString(parts[0]);
        String itemName = parts[1];
        String status = parts[2];
        String errorMessage = parts.length > 3 ? parts[3] : null;

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));

        if ("SUCCESS".equals(status)) {
            try {
                processingStateService.incrementProcessed(documentId);
                if (processingStateService.isProcessingComplete(documentId)) {
                    documentStateManager.updateStatus(documentId, "PROCESSED");
                }
            } catch (Exception e) {
                logger.warn("Failed to update processing state for document {}. State may be inconsistent.", documentId, e);
                // Здесь можно добавить дополнительную логику, например, повторную попытку
            }
        } else {
            documentStateManager.updateStatus(documentId, "FAILED",
                    String.format("Transaction '%s' failed: %s", itemName, errorMessage));
            processingStateService.clearState(documentId);
        }
    }

    private String determineErrorMessage(Exception e) {
        if (e.getMessage().contains("Tesseract")) {
            return "Failed to recognize text in the image. Try uploading a clearer photo.";
        } else if (e.getMessage().contains("OpenAI")) {
            return "Failed to classify purchases. Check the receipt quality.";
        } else {
            return "Processing error: " + e.getMessage();
        }
    }

    private void sendToDlq(String message, String errorReason) {
        Message<String> dlqMessage = MessageBuilder
                .withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, "document-processing-dlq")
                .setHeader("errorReason", errorReason)
                .build();
        kafkaTemplate.send(dlqMessage);
        logger.info("Message sent to DLQ: {}", message);
    }
}