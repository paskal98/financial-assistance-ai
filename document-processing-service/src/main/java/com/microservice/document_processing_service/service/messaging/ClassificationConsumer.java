package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.model.dto.OcrResultMessage;
import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import com.microservice.document_processing_service.service.TransactionProducerService;
import com.microservice.document_processing_service.service.ai.OpenAiClassifier;
import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import com.microservice.document_processing_service.utils.CompressionUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ClassificationConsumer.class);

    private final OpenAiClassifier openAiClassifier;
    private final DocumentStateManager documentStateManager;
    private final TransactionProducerService transactionProducerService;

    @KafkaListener(topics = "classification-queue", groupId = "classification-group",
            containerFactory = "ocrResultKafkaListenerContainerFactory")
    public void processClassification(OcrResultMessage message) {
        String ocrText = message.isCompressed() ? CompressionUtils.decompress(message.getOcrText()) : message.getOcrText();
        UUID documentId = message.getDocumentId();
        UUID userId = message.getUserId();
        String date = message.getDate();

        try {
            logger.info("Starting classification for document: {}", documentId);
            documentStateManager.updateStatus(documentId, "CLASSIFYING");

            List<TransactionItemDto> items = openAiClassifier.classifyItems(ocrText, documentId);
            if (!Objects.equals(date, "")) {
                Instant parsedDate = Instant.parse(date);
                items.forEach(item -> item.setDate(parsedDate));
            }

            items.forEach(item -> transactionProducerService.sendTransaction(item, userId, documentId));
            logger.info("Classification completed for document: {}, {} items", documentId, items.size());
        } catch (Exception e) {
            logger.error("Classification failed for document: {}", documentId, e);
            documentStateManager.updateStatus(documentId, "FAILED", "Classification error: " + e.getMessage());
        }
    }
}