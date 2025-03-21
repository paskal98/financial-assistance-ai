package com.microservice.document_processing_service.service.ocr;

import com.microservice.document_processing_service.exception.DocumentProcessingException;
import lombok.RequiredArgsConstructor;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TesseractOcrProcessor implements OcrProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TesseractOcrProcessor.class);

    private final FileDownloader fileDownloader;

    @Value("${tesseract.tessdata.path:C:/Program Files/Tesseract-OCR/tessdata}")
    private String tessDataPath;

    @Override
    public String extractTextFromImage(String objectName, UUID documentId) {
        ITesseract tesseract = new Tesseract();

        try {
            File tessDataDir = new File(tessDataPath);
            if (!tessDataDir.exists() || !tessDataDir.isDirectory()) {
                logger.error("Tessdata directory not found at: {} for document: {}", tessDataPath, documentId);
                throw new DocumentProcessingException("Tessdata directory not found at: " + tessDataPath + " for document: " + documentId);
            }
            tesseract.setDatapath(tessDataPath);

            Path tempFile = fileDownloader.downloadFile(objectName, documentId);

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
}