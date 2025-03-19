package com.microservice.document_processing_service.service;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

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
            // Разделяем путь файла и дату (если есть)
            String[] parts = message.split("\\|", 2);
            String filePath = parts[0];
            String date = parts.length > 1 ? parts[1] : null;

            logger.info("Processing document: {}", filePath);

            // OCR
            String ocrText = ocrService.extractTextFromImage(filePath);

            // AI-классификация
            List<TransactionItemDto> items = aiClassificationService.classifyItems(ocrText);
            if (date != null) {
                items.forEach(item -> item.setDate(date));
            }

            // Отправка в Kafka (transactions-topic)
            items.forEach(transactionProducerService::sendTransaction);

            logger.info("Document processed successfully: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to process document: {}", message, e);
            // Можно добавить повторную отправку в "dead letter queue" при ошибке
        }
    }
}