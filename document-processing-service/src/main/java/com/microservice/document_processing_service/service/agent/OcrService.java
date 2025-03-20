package com.microservice.document_processing_service.service.agent;

import com.microservice.document_processing_service.exception.DocumentProcessingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OcrService {
    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    private final MinioClient minioClient;

    @Value("${tesseract.tessdata.path:C:/Program Files/Tesseract-OCR/tessdata}")
    private String tessDataPath;

    @Value("${minio.bucket}")
    private String bucketName;

    // Note: Tesseract (tess4j) natively supports PDF files with text or images
    @CircuitBreaker(name = "minio-cb", fallbackMethod = "fallbackOcr")
    public String extractTextFromImage(String objectName, UUID documentId) {
        ITesseract tesseract = new Tesseract();

        try {
            File tessDataDir = new File(tessDataPath);
            if (!tessDataDir.exists() || !tessDataDir.isDirectory()) {
                logger.error("Tessdata directory not found at: {} for document: {}", tessDataPath, documentId);
                throw new DocumentProcessingException("Tessdata directory not found at: " + tessDataPath + " for document: " + documentId);
            }
            tesseract.setDatapath(tessDataPath);

            Path tempFile = Files.createTempFile("ocr-", objectName.substring(objectName.lastIndexOf(".")));
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build())) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("Extracting text from image: {} for document: {}", objectName, documentId);
            String result = tesseract.doOCR(tempFile.toFile());
            logger.info("Text extracted successfully for document {}: {}", documentId, result);

            Files.deleteIfExists(tempFile);
            return result;

        } catch (Exception e) {
            logger.error("Failed to extract text from image: {} for document: {}", objectName, documentId, e);
            throw new DocumentProcessingException("Failed to process image with Tesseract for document: " + documentId, e);
        }
    }

    private String fallbackOcr(String objectName, UUID documentId, Throwable t) {
        logger.warn("Fallback triggered for OCR for document {} due to: {}", documentId, t.getMessage());
        throw new DocumentProcessingException("OCR processing failed for document " + documentId + ": " + t.getMessage(), t);
    }
}