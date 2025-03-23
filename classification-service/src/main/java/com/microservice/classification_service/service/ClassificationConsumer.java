package com.microservice.classification_service.service;

import com.microservice.classification_service.model.dto.OcrResultMessage;
import com.microservice.classification_service.model.dto.TransactionItemDto;
import com.microservice.classification_service.utils.CompressionUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ClassificationConsumer.class);

    private final OpenAiClassifier openAiClassifier;
    private final KafkaTemplate<String, TransactionItemDto> transactionKafkaTemplate;

    @KafkaListener(topics = "classification-queue", groupId = "classification-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void processClassification(OcrResultMessage message,
                                      @Header("userId") String userIdHeader) {
        String ocrText = message.isCompressed() ? CompressionUtils.decompress(message.getOcrText()) : message.getOcrText();
        UUID documentId = message.getDocumentId();
        UUID userId = message.getUserId(); // Используем из сообщения

        try {
            logger.info("Starting classification for document: {}", documentId);
            List<TransactionItemDto> items = openAiClassifier.classifyItems(ocrText, documentId);
            if (message.getDate() != null) {
                Instant parsedDate = Instant.parse(message.getDate());
                items.forEach(item -> item.setDate(parsedDate));
            }

            items.forEach(item -> {
                item.setUserId(userId); // Устанавливаем userId
                ProducerRecord<String, TransactionItemDto> record = new ProducerRecord<>("transactions-topic", item);
                record.headers().add("userId", userId.toString().getBytes(StandardCharsets.UTF_8));
                transactionKafkaTemplate.send(record);
                logger.info("Sent transaction for document: {} - {}", documentId, item.getName());
            });

            logger.info("Classification completed for document: {}, {} items", documentId, items.size());
        } catch (Exception e) {
            logger.error("Classification failed for document: {}", documentId, e);
        }
    }
}
