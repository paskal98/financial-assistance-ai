package com.microservice.document_processing_service.service.core;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DocumentStorageTest {

    @Mock private MinioClient minioClient;
    @Mock private MultipartFile file;
    private DocumentStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        storageService = new DocumentStorageServiceImpl(minioClient);
        storageService.bucketName = "documents";

        when(file.getOriginalFilename()).thenReturn("test.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L);
    }

    @Test
    void store_Success() throws Exception {
        // Arrange
        InputStream inputStream = mock(InputStream.class);
        when(file.getInputStream()).thenReturn(inputStream);

        // Act
        String filePath = storageService.store(file);

        // Assert
        assertNotNull(filePath);
        assertTrue(filePath.endsWith(".pdf"));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void store_EmptyFile_ThrowsException() {
        // Arrange
        when(file.isEmpty()).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> storageService.store(file));
        assertEquals("File cannot be null or empty", exception.getMessage());
        try {
            verify(minioClient, never()).putObject(any());
        } catch (ErrorResponseException e) {
            throw new RuntimeException(e);
        } catch (InsufficientDataException e) {
            throw new RuntimeException(e);
        } catch (InternalException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (InvalidResponseException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        } catch (XmlParserException e) {
            throw new RuntimeException(e);
        }
    }
}