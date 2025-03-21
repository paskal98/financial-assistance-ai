package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.model.dto.OcrResultMessage;
import com.microservice.document_processing_service.service.ocr.FileDownloader;
import com.microservice.document_processing_service.service.ocr.TesseractOcrProcessor;
import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import com.microservice.document_processing_service.utils.CompressionUtils;
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
    private final DocumentStateManager documentStateManager;
    private final FileDownloader fileDownloader;
    private final KafkaTemplate<String, OcrResultMessage> ocrResultKafkaTemplate;

    @KafkaListener(topics = "ocr-processing-queue", groupId = "ocr-processing-group", concurrency = "10")
    public void processOcr(String message) {
        String[] parts = message.split("\\|", 4);
        String filePath = parts[0];
        UUID documentId = UUID.fromString(parts[1]);
        UUID userId = UUID.fromString(parts[2]);
        String date = parts.length > 3 ? parts[3] : null;

        try {
            logger.info("Starting OCR for document: {}", documentId);
            documentStateManager.updateStatus(documentId, "EXTRACTING_TEXT");

            String contentType = determineContentType(filePath);
            InputStream inputStream = fileDownloader.downloadFile(filePath, documentId);
            String ocrText = tesseractOcrProcessor.extractText(inputStream, filePath, documentId, contentType);

            // Сжимаем текст, если он большой
            String processedOcrText = CompressionUtils.compress(ocrText);
            boolean isCompressed = !processedOcrText.equals(ocrText); // Сжатие произошло, если текст изменился

            // Формируем JSON-сообщение
            OcrResultMessage resultMessage = new OcrResultMessage();
            resultMessage.setOcrText(processedOcrText);
            resultMessage.setDocumentId(documentId);
            resultMessage.setUserId(userId);
            resultMessage.setDate(date);
            resultMessage.setCompressed(isCompressed);

            ocrResultKafkaTemplate.send("classification-queue", resultMessage);
            logger.info("OCR completed for document: {}, compressed: {}", documentId, isCompressed);
        } catch (Exception e) {
            logger.error("OCR failed for document: {}", documentId, e);
            documentStateManager.updateStatus(documentId, "FAILED", "OCR error: " + e.getMessage());
        }
    }

    private String determineContentType(String filePath) {
        if (filePath.endsWith(".pdf")) return "application/pdf";
        else if (filePath.endsWith(".png")) return "image/png";
        else if (filePath.endsWith(".jpeg") || filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }
}