package com.microservice.document_processing_service.service.processing;

import com.microservice.document_processing_service.model.entity.Document;
import com.microservice.document_processing_service.repository.DocumentRepository;
import com.microservice.document_processing_service.service.messaging.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentStateManager {
    private static final Logger logger = LoggerFactory.getLogger(DocumentStateManager.class);

    private final DocumentRepository documentRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    public Document updateStatus(UUID documentId, String status, String errorMessage) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalStateException("Document not found: " + documentId));

        document.setStatus(status);
        document.setErrorMessage(errorMessage);
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);

        logger.info("Updated document {} status to {}", documentId, status);
        webSocketNotificationService.sendStatusUpdate(document);

        return document;
    }

    public Document updateStatus(UUID documentId, String status) {
        return updateStatus(documentId, status, null);
    }
}