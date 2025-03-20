package com.microservice.document_processing_service.service.event_driven;

import com.microservice.document_processing_service.model.entity.Document;
import com.microservice.document_processing_service.repository.DocumentRepository;
import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import com.microservice.document_processing_service.service.TransactionProducerService;
import com.microservice.document_processing_service.service.agent.AiClassificationService;
import com.microservice.document_processing_service.service.agent.OcrService;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    private final OcrService ocrService;
    private final AiClassificationService aiClassificationService;
    private final TransactionProducerService transactionProducerService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DocumentRepository documentRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    private final Map<UUID, DocumentProcessingState> processingStates = new HashMap<>();

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            exclude = {IllegalArgumentException.class}
    )
    @KafkaListener(topics = "document-processing-queue", groupId = "doc-processing-group")
    public void processDocument(String message) {
        String[] parts = message.split("\\|", 4);
        String filePath = parts[0];
        String userId = parts[1];
        String documentId = parts[2];
        String date = parts.length > 3 ? parts[3] : null;

        UUID docId = UUID.fromString(documentId);
        Document document = documentRepository.findById(docId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));

        try {
            logger.info("Processing document: {}", filePath);

            document.setStatus("PROCESSING");
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            webSocketNotificationService.sendStatusUpdate(document);

            document.setStatus("EXTRACTING_TEXT");
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            webSocketNotificationService.sendStatusUpdate(document);
            String ocrText = ocrService.extractTextFromImage(filePath, docId);

            document.setStatus("CLASSIFYING");
            documentRepository.save(document);
            webSocketNotificationService.sendStatusUpdate(document);
            List<TransactionItemDto> items = aiClassificationService.classifyItems(ocrText, docId);
            if (date != null) {
                items.forEach(item -> item.setDate(Instant.parse(date)));
            }

            processingStates.put(docId, new DocumentProcessingState(items.size()));
            items.forEach(item -> transactionProducerService.sendTransaction(item, UUID.fromString(userId), docId));

            logger.info("Document sent for transaction processing: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to process document: {}", message, e);
            document.setStatus("FAILED");
            if (e.getMessage().contains("Tesseract")) {
                document.setErrorMessage("Failed to recognize text in the image. Try uploading a clearer photo.");
            } else if (e.getMessage().contains("OpenAI")) {
                document.setErrorMessage("Failed to classify purchases. Check the receipt quality.");
            } else {
                document.setErrorMessage("Processing error: " + e.getMessage());
            }
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            webSocketNotificationService.sendStatusUpdate(document);
            sendToDlq(message, e.getMessage());
            processingStates.remove(docId);
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
        DocumentProcessingState state = processingStates.get(documentId);

        if (state == null) {
            logger.warn("No processing state found for document: {}", documentId);
            return;
        }

        if ("SUCCESS".equals(status)) {
            state.incrementProcessed();
        } else {
            document.setStatus("FAILED");
            document.setErrorMessage(String.format("Transaction '%s' failed: %s", itemName, errorMessage));
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            webSocketNotificationService.sendStatusUpdate(document);
            processingStates.remove(documentId);
            return;
        }

        if (state.isProcessingComplete()) {
            document.setStatus("PROCESSED");
            document.setErrorMessage(null);
            document.setUpdatedAt(Instant.now());
            documentRepository.save(document);
            webSocketNotificationService.sendStatusUpdate(document);
            processingStates.remove(documentId);
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