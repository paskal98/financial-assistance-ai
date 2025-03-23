package com.microservcie.ocr_service.service;

import com.microservcie.ocr_service.model.dto.OcrResultMessage;
import com.microservcie.ocr_service.utils.CompressionUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OcrProcessingConsumer {
    private static final Logger logger = LoggerFactory.getLogger(OcrProcessingConsumer.class);

    private final TesseractOcrProcessor tesseractOcrProcessor;
    private final FileDownloader fileDownloader;
    private final KafkaTemplate<String, OcrResultMessage> ocrResultKafkaTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate; // Добавляем для feedback

    @KafkaListener(topics = "ocr-processing-queue", groupId = "ocr-processing-group", concurrency = "20")
    public void processOcr(@Payload String filePath,
                           @Header("userId") String userId,
                           @Header("documentId") String documentId,
                           @Header(name = "date", required = false) String date) {
        UUID parsedUserId = UUID.fromString(userId);
        UUID parsedDocumentId = UUID.fromString(documentId);

        try {
            logger.info("Starting OCR for document: {}", parsedDocumentId);
            kafkaTemplate.send("ocr-feedback-queue", String.format("%s|EXTRACTING_TEXT", parsedDocumentId));

            InputStream inputStream = fileDownloader.downloadFile(filePath, parsedDocumentId);
            String ocrText = tesseractOcrProcessor.extractText(inputStream, filePath, parsedDocumentId);

            String processedOcrText = CompressionUtils.compress(ocrText);
            boolean isCompressed = !processedOcrText.equals(ocrText);

            OcrResultMessage resultMessage = new OcrResultMessage();
            resultMessage.setOcrText(processedOcrText);
            resultMessage.setDocumentId(parsedDocumentId);
            resultMessage.setUserId(parsedUserId);
            resultMessage.setDate(date);
            resultMessage.setCompressed(isCompressed);

            ProducerRecord<String, OcrResultMessage> record = new ProducerRecord<>("classification-queue", resultMessage);
            record.headers().add("userId", userId.getBytes(StandardCharsets.UTF_8));
            ocrResultKafkaTemplate.send(record);

            // Уведомляем о переходе в CLASSIFYING
            kafkaTemplate.send("ocr-feedback-queue", String.format("%s|CLASSIFYING", parsedDocumentId));
            logger.info("OCR completed for document: {}, compressed: {}", parsedDocumentId, isCompressed);
        } catch (Exception e) {
            logger.error("OCR failed for document: {}", parsedDocumentId, e);
            String feedbackMessage = String.format("%s|FAILED|OCR error: %s", parsedDocumentId, e.getMessage());
            kafkaTemplate.send("ocr-feedback-queue", feedbackMessage);
        }
    }
}