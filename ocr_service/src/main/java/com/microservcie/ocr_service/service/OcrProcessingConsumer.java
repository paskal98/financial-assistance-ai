package com.microservcie.ocr_service.service;

import com.microservcie.ocr_service.model.dto.OcrResultMessage;
import com.microservcie.ocr_service.utils.CompressionUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
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
    public void processOcr(String message) {
        String[] parts = message.split("\\|", 4);
        String filePath = parts[0];
        UUID userId = UUID.fromString(parts[1]);
        UUID documentId = UUID.fromString(parts[2]);
        String date = parts.length > 3 ? parts[3] : null;

        try {
            logger.info("Starting OCR for document: {}", documentId);
            kafkaTemplate.send("ocr-feedback-queue", String.format("%s|EXTRACTING_TEXT", documentId));

            InputStream inputStream = fileDownloader.downloadFile(filePath, documentId);
            String ocrText = tesseractOcrProcessor.extractText(inputStream, filePath, documentId);

            String processedOcrText = CompressionUtils.compress(ocrText);
            boolean isCompressed = !processedOcrText.equals(ocrText);

            OcrResultMessage resultMessage = new OcrResultMessage();
            resultMessage.setOcrText(processedOcrText);
            resultMessage.setDocumentId(documentId);
            resultMessage.setUserId(userId);
            resultMessage.setDate(date);
            resultMessage.setCompressed(isCompressed);

            ocrResultKafkaTemplate.send("classification-queue", resultMessage);
            logger.info("OCR completed for document: {}, compressed: {}", documentId, isCompressed);
            System.out.println(ocrText);
        } catch (Exception e) {
            logger.error("OCR failed for document: {}", documentId, e);
            String feedbackMessage = String.format("%s|FAILED|OCR error: %s", documentId, e.getMessage());
            kafkaTemplate.send("ocr-feedback-queue", feedbackMessage);
        }
    }
}