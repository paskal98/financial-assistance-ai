package com.microservice.document_processing_service.service.core;

import com.microservice.document_processing_service.model.entity.Document;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DocumentProcessingTest extends BaseDocumentProcessingTest {

    @Test
    void processDocuments_Success() {
        // Arrange
        when(storageService.store(file)).thenReturn("test.pdf");
        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setUserId(UUID.fromString(userId));
        document.setFilePath("test.pdf");
        document.setStatus("PENDING");
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        when(documentRepository.save(any(Document.class))).thenReturn(document);

        // Act
        List<String> responses = documentProcessingService.processDocuments(List.of(file), userId, null);

        // Assert
        assertEquals(1, responses.size());
        assertTrue(responses.get(0).contains("Document queued for processing"));
        verify(storageService).store(file);
        verify(documentRepository).save(any(Document.class));
        verify(documentKafkaTemplate).send(any(ProducerRecord.class));
        verify(webSocketNotificationService).sendStatusUpdate(any(Document.class));
        verify(documentStateManager).updateStatus(any(UUID.class), eq("PROCESSING"));
    }

    @Test
    void processDocuments_InvalidFileType_ThrowsException() {
        // Arrange
        when(file.getContentType()).thenReturn("text/plain");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> documentProcessingService.processDocuments(List.of(file), userId, null));
        assertEquals("Unsupported file type: text/plain", exception.getMessage());
        verify(storageService, never()).store(any());
    }

    @Test
    void processDocuments_TooManyFiles_ThrowsException() {
        // Arrange
        List<MultipartFile> files = List.of(file, file, file, file, file, file); // 6 файлов

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> documentProcessingService.processDocuments(files, userId, null));
        assertEquals("Maximum 5 files allowed", exception.getMessage());
        verify(storageService, never()).store(any());
    }
}