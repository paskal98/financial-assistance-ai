package com.microservice.document_processing_service.service.event_driven;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import com.microservice.document_processing_service.service.TransactionProducerService;
import com.microservice.document_processing_service.service.agent.AiClassificationService;
import com.microservice.document_processing_service.service.agent.OcrService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    private final OcrService ocrService;
    private final AiClassificationService aiClassificationService;
    private final TransactionProducerService transactionProducerService;

    @KafkaListener(topics = "document-processing-queue", groupId = "doc-processing-group")
    public void processDocument(String message) {
        try {
            String[] parts = message.split("\\|", 4);
            String filePath = parts[0];
            String userId = parts[1];
            String documentId = parts[2];
            String date = parts.length > 3 ? parts[3] : null;

            logger.info("Processing document: {}", filePath);

            // OCR
            String ocrText = ocrService.extractTextFromImage(filePath);

            // AI Classification
            List<TransactionItemDto> items = aiClassificationService.classifyItems(ocrText);
            if (date != null) {
                items.forEach(item -> item.setDate(Instant.parse(date)));
            }

            // Send to Kafka with userId and documentId
            items.forEach(item -> transactionProducerService.sendTransaction(item, UUID.fromString(userId), UUID.fromString(documentId)));

            logger.info("Document processed successfully: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to process document: {}", message, e);
            // Optionally, send to a dead-letter queue
        }
    }
}