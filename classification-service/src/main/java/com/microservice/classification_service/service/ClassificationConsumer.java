package com.microservice.classification_service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.classification_service.model.dto.TransactionItemDto;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.shared.dto.FeedbackMessage;
import org.shared.utils.KafkaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ClassificationConsumer {
    private static final Logger logger = LoggerFactory.getLogger(ClassificationConsumer.class);
    private static final String FEEDBACK_TOPIC = "document-feedback-queue";

    private final OpenAiClassifier openAiClassifier;
    private final KafkaTemplate<String, TransactionItemDto> transactionKafkaTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${minio.bucket}")
    private String bucketName;

    @KafkaListener(topics = "classification-queue", groupId = "classification-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void processClassification(String messageJson,
                                      @Header("userId") String userIdHeader) {
        try {
            Map<String, String> message = objectMapper.readValue(messageJson, new TypeReference<>() {});
            UUID documentId = UUID.fromString(message.get("documentId"));
            UUID userId = UUID.fromString(message.get("userId"));
            String textPath = message.get("textPath");
            String date = message.get("date");

            logger.info("Starting classification for document: {}", documentId);
            FeedbackMessage startFeedback = new FeedbackMessage(documentId.toString(), "CLASSIFICATION", "STARTED", null);
            sendFeedbackAndHandle(kafkaTemplate, FEEDBACK_TOPIC, startFeedback, documentId, "start");


            String ocrText;
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(textPath)
                            .build())) {
                ocrText = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            logger.info("Loaded OCR text from MinIO for document: {}", documentId);

            List<TransactionItemDto> items = openAiClassifier.classifyItems(ocrText, documentId);
            if (date != null) {
                Instant parsedDate = Instant.parse(date);
                items.forEach(item -> item.setDate(parsedDate));
            }

            FeedbackMessage itemsFeedback = new FeedbackMessage(documentId.toString(), "CLASSIFICATION", "ITEMS_COUNT", String.valueOf(items.size()));
            sendFeedbackAndHandle(kafkaTemplate, FEEDBACK_TOPIC, itemsFeedback, documentId, "items count");

            items.forEach(item -> {
                item.setUserId(userId);
                ProducerRecord<String, TransactionItemDto> record = new ProducerRecord<>("transactions-topic", item);
                record.headers().add("userId", userId.toString().getBytes(StandardCharsets.UTF_8));
                transactionKafkaTemplate.send(record);
                logger.info("Sent transaction for document: {} - {}", documentId, item.getName());
            });

            logger.info("Classification completed for document: {}, {} items", documentId, items.size());
        } catch (Exception e) {
            logger.error("Classification failed: {}", e.getMessage(), e);
            UUID documentId = extractDocumentIdFromMessage(messageJson); // Вспомогательный метод
            FeedbackMessage failureFeedback = new FeedbackMessage(documentId.toString(), "CLASSIFICATION", "FAILED", "Classification error: " + e.getMessage());
            sendFeedbackAndHandle(kafkaTemplate, FEEDBACK_TOPIC, failureFeedback, documentId, "failure");
        }
    }

    private UUID extractDocumentIdFromMessage(String messageJson) {
        try {
            Map<String, String> message = objectMapper.readValue(messageJson, new TypeReference<>() {});
            return UUID.fromString(message.get("documentId"));
        } catch (Exception e) {
            return UUID.randomUUID(); // Fallback, если не удалось извлечь
        }
    }

    private void sendFeedbackAndHandle(KafkaTemplate<String, String> kafkaTemplate, String topic,
                                       FeedbackMessage message, UUID documentId, String phase) {
        CompletableFuture<SendResult<String, String>> future = KafkaUtils.sendFeedback(kafkaTemplate, topic, message);
        future.handle((result, ex) -> {
            if (ex != null) {
                logger.error("Failed to send {} feedback for document: {}, error: {}", phase, documentId, ex.getMessage(), ex);
            } else {
                logger.debug("Successfully sent {} feedback for document: {}, offset: {}", phase, documentId,
                        result.getRecordMetadata().offset());
            }
            return null;
        });
    }
}