package com.microservice.document_processing_service.service.event_driven;

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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessingConsumer {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    private final OcrService ocrService;
    private final AiClassificationService aiClassificationService;
    private final TransactionProducerService transactionProducerService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            exclude = {IllegalArgumentException.class}
    )
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

            // Send to Kafka
            items.forEach(item -> transactionProducerService.sendTransaction(item, UUID.fromString(userId), UUID.fromString(documentId)));

            logger.info("Document processed successfully: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to process document: {}", message, e);
            sendToDlq(message, e.getMessage());
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