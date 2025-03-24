package com.microservcie.ocr_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OcrProcessingConsumer {
    private static final Logger logger = LoggerFactory.getLogger(OcrProcessingConsumer.class);
    private static final String FEEDBACK_TOPIC = "document-feedback-queue";

    private final TesseractOcrProcessor tesseractOcrProcessor;
    private final FileDownloader fileDownloader;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Value("${minio.bucket}")
    private String bucketName;

    @KafkaListener(topics = "ocr-processing-queue", groupId = "ocr-processing-group", concurrency = "20")
    public void processOcr(@Payload String filePath,
                           @Header("userId") String userId,
                           @Header("documentId") String documentId,
                           @Header(name = "date", required = false) String date) {
        UUID parsedUserId = UUID.fromString(userId);
        UUID parsedDocumentId = UUID.fromString(documentId);

        try {
            logger.info("Starting OCR for document: {}", parsedDocumentId);
            FeedbackMessage startFeedback = new FeedbackMessage(documentId, "OCR", "EXTRACTING_TEXT", null);
            sendFeedbackAndHandle(kafkaTemplate, FEEDBACK_TOPIC, startFeedback, parsedDocumentId, "start");

            InputStream inputStream = fileDownloader.downloadFile(filePath, parsedDocumentId);
            String ocrText = tesseractOcrProcessor.extractText(inputStream, filePath, parsedDocumentId);

            String textObjectName = "ocr-text/" + parsedDocumentId + ".txt";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(textObjectName)
                            .stream(new ByteArrayInputStream(ocrText.getBytes(StandardCharsets.UTF_8)), ocrText.length(), -1)
                            .contentType("text/plain")
                            .build()
            );
            logger.info("OCR text saved to MinIO: {}", textObjectName);

            String message = objectMapper.writeValueAsString(Map.of(
                    "documentId", parsedDocumentId.toString(),
                    "userId", parsedUserId,
                    "textPath", textObjectName,
                    "date", date != null ? date : Instant.now().toString()
            ));
            ProducerRecord<String, String> record = new ProducerRecord<>("classification-queue", message);
            record.headers().add("userId", userId.getBytes(StandardCharsets.UTF_8));
            kafkaTemplate.send(record);

            FeedbackMessage successFeedback = new FeedbackMessage(documentId, "OCR", "CLASSIFYING", null);
            sendFeedbackAndHandle(kafkaTemplate, FEEDBACK_TOPIC, successFeedback, parsedDocumentId, "success");
            logger.info("OCR completed for document: {}", parsedDocumentId);
        } catch (Exception e) {
            logger.error("OCR failed for document: {}", parsedDocumentId, e);
            FeedbackMessage failureFeedback = new FeedbackMessage(documentId, "OCR", "FAILED", "OCR error: " + e.getMessage());
            sendFeedbackAndHandle(kafkaTemplate, FEEDBACK_TOPIC, failureFeedback, parsedDocumentId, "failure");
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