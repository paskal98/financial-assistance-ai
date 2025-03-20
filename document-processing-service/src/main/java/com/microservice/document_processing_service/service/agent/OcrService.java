package com.microservice.document_processing_service.service.agent;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class OcrService {
    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    @Value("${tesseract.tessdata.path:C:/Program Files/Tesseract-OCR/tessdata}")
    private String tessDataPath;

    public String extractTextFromImage(String filePath) {
        ITesseract tesseract = new Tesseract();

        try {
            // Проверяем, существует ли директория tessdata
            File tessDataDir = new File(tessDataPath);
            if (!tessDataDir.exists() || !tessDataDir.isDirectory()) {
                logger.error("Tessdata directory not found at: {}", tessDataPath);
                throw new IllegalStateException("Tessdata directory not found at: " + tessDataPath);
            }

            // Устанавливаем путь к данным Tesseract
            tesseract.setDatapath(tessDataPath);
            logger.info("Extracting text from image: {}", filePath);

            // Проверяем, существует ли файл
            File imageFile = new File(filePath);
            if (!imageFile.exists()) {
                logger.error("Image file not found: {}", filePath);
                throw new IllegalArgumentException("Image file not found: " + filePath);
            }

            // Извлекаем текст
            String result = tesseract.doOCR(imageFile);
            logger.info("Text extracted successfully: {}", result);
            return result;

        } catch (TesseractException e) {
            logger.error("Failed to extract text from image: {}", filePath, e);
            throw new RuntimeException("Failed to process image with Tesseract: " + e.getMessage(), e);
        }
    }
}