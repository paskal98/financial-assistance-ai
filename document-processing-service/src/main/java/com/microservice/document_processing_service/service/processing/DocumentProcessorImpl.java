package com.microservice.document_processing_service.service.processing;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessorImpl implements DocumentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessorImpl.class);
    private final DocumentStateManager documentStateManager;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.document-processing}")
    private String documentProcessingTopic;

    @Override
    public List<TransactionItemDto> processDocument(String filePath, UUID documentId, UUID userId, String date) {
        logger.info("Processing document: {} for documentId: {}", filePath, documentId);

        documentStateManager.updateStatus(documentId, "QUEUED_FOR_OCR");
        String ocrMessage = String.format("%s|%s|%s|%s", filePath, documentId, userId, date != null ? date : "");
        kafkaTemplate.send(documentProcessingTopic, ocrMessage);

        return Collections.emptyList();
    }
}