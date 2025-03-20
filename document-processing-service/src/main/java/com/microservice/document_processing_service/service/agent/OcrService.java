package com.microservice.document_processing_service.service.agent;

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

@Service
@RequiredArgsConstructor
public class OcrService {
    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    private final MinioClient minioClient;

    @Value("${tesseract.tessdata.path:C:/Program Files/Tesseract-OCR/tessdata}")
    private String tessDataPath;

    @Value("${minio.bucket}")
    private String bucketName;

    @CircuitBreaker(name = "minio-cb", fallbackMethod = "fallbackOcr")
    public String extractTextFromImage(String objectName) {
        ITesseract tesseract = new Tesseract();

        try {
            // Проверяем tessdata
            File tessDataDir = new File(tessDataPath);
            if (!tessDataDir.exists() || !tessDataDir.isDirectory()) {
                logger.error("Tessdata directory not found at: {}", tessDataPath);
                throw new IllegalStateException("Tessdata directory not found at: " + tessDataPath);
            }
            tesseract.setDatapath(tessDataPath);

            // Загружаем файл из MinIO во временный файл
            Path tempFile = Files.createTempFile("ocr-", objectName.substring(objectName.lastIndexOf(".")));
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build())) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("Extracting text from image: {}", objectName);

            // Извлекаем текст
            String result = tesseract.doOCR(tempFile.toFile());
            logger.info("Text extracted successfully: {}", result);

            // Удаляем временный файл
            Files.deleteIfExists(tempFile);

            return result;

        } catch (Exception e) {
            logger.error("Failed to extract text from image: {}", objectName, e);
            throw new RuntimeException("Failed to process image with Tesseract", e);
        }
    }


    private String fallbackOcr(String objectName, Throwable t) {
        logger.warn("Fallback triggered for OCR due to: {}", t.getMessage());
        return "OCR processing unavailable";
    }
}