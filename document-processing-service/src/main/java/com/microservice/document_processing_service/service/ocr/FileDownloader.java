package com.microservice.document_processing_service.service.ocr;

import com.microservice.document_processing_service.exception.DocumentProcessingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FileDownloader {
    private static final Logger logger = LoggerFactory.getLogger(FileDownloader.class);

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @CircuitBreaker(name = "minio-cb", fallbackMethod = "fallbackDownload")
    public InputStream downloadFile(String objectName, UUID documentId) {
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            logger.info("Stream retrieved from MinIO: {} for document: {}", objectName, documentId);
            return inputStream;
        } catch (Exception e) {
            logger.error("Failed to retrieve stream from MinIO: {} for document: {}", objectName, documentId, e);
            throw new DocumentProcessingException("Failed to download file from MinIO for document: " + documentId, e);
        }
    }

    private InputStream fallbackDownload(String objectName, UUID documentId, Throwable t) {
        logger.warn("Fallback triggered for file download for document {} due to: {}", documentId, t.getMessage());
        throw new DocumentProcessingException("File download failed for document " + documentId + ": " + t.getMessage(), t);
    }
}