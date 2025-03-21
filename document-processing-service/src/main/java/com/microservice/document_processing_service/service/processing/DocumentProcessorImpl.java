package com.microservice.document_processing_service.service.processing;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import com.microservice.document_processing_service.service.ai.OpenAiClassifier;
import com.microservice.document_processing_service.service.ocr.FileDownloader;
import com.microservice.document_processing_service.service.ocr.TesseractOcrProcessor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessorImpl implements DocumentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessorImpl.class);

    private final OpenAiClassifier openAiClassifier;
    private final DocumentStateManager documentStateManager;
    private final FileDownloader fileDownloader;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public List<TransactionItemDto> processDocument(String filePath, UUID documentId, UUID userId, String date) {
        logger.info("Processing document: {} for documentId: {}", filePath, documentId);

        // Отправляем задачу на OCR в отдельную очередь
        documentStateManager.updateStatus(documentId, "QUEUED_FOR_OCR");
        String ocrMessage = String.format("%s|%s|%s|%s", filePath, documentId, userId, date != null ? date : "");
        kafkaTemplate.send("ocr-processing-queue", ocrMessage);

        // Возвращаем пустой список, так как результат будет позже
        return Collections.emptyList();
    }

    private String determineContentType(String filePath) {
        if (filePath.endsWith(".pdf")) return "application/pdf";
        else if (filePath.endsWith(".png")) return "image/png";
        else if (filePath.endsWith(".jpeg") || filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }
}