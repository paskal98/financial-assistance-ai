package com.microservice.document_processing_service.service.core;

import com.microservice.document_processing_service.repository.DocumentRepository;
import com.microservice.document_processing_service.service.DocumentStorageService;
import com.microservice.document_processing_service.service.messaging.WebSocketNotificationService;
import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class BaseDocumentProcessingTest {

    @Mock protected DocumentStorageService storageService;
    @Mock protected KafkaTemplate<String, String> documentKafkaTemplate;
    @Mock protected DocumentRepository documentRepository;
    @Mock protected WebSocketNotificationService webSocketNotificationService;
    @Mock protected DocumentStateManager documentStateManager;

    protected DocumentProcessingServiceImpl documentProcessingService;
    protected MultipartFile file;
    protected String userId;

    @BeforeEach
    void baseSetUp() {
        MockitoAnnotations.openMocks(this);
        documentProcessingService = new DocumentProcessingServiceImpl(
                storageService, documentKafkaTemplate, documentRepository,
                webSocketNotificationService, documentStateManager
        );
        file = mock(MultipartFile.class);
        userId = UUID.randomUUID().toString();

        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L); // 1KB
    }
}