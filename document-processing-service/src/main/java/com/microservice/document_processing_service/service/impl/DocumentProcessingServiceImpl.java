package com.microservice.document_processing_service.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingServiceImpl.class);
    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "application/pdf");

    private final DocumentStorageService storageService;
    private final KafkaTemplate<String, String> documentKafkaTemplate;

    @Override
    public List<String> processDocuments(List<MultipartFile> files, String userId, String date) {
        // Валидация количества файлов
        if (files == null || files.isEmpty()) {
            logger.warn("No files provided for upload by user: {}", userId);
            throw new IllegalArgumentException("No files provided");
        }
        if (files.size() > MAX_FILES) {
            logger.warn("Too many files uploaded: {}. Maximum allowed: {}", files.size(), MAX_FILES);
            throw new IllegalArgumentException("Maximum " + MAX_FILES + " files allowed");
        }

        // Валидация размера и типа файлов
        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
                logger.warn("Unsupported file type '{}' for file '{}'", contentType, file.getOriginalFilename());
                throw new IllegalArgumentException("Unsupported file type: " + contentType);
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                logger.warn("File '{}' exceeds size limit of {} bytes", file.getOriginalFilename(), MAX_FILE_SIZE);
                throw new IllegalArgumentException("File '" + file.getOriginalFilename() + "' exceeds size limit of 5MB");
            }
        }

        List<String> responses = new ArrayList<>();
        String uuidRegex = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
        Pattern pattern = Pattern.compile(uuidRegex);

        // Обработка каждого файла
        for (MultipartFile file : files) {
            try {
                // Сохраняем файл
                String filePath = storageService.store(file);

                // Извлекаем или генерируем documentId
                Matcher matcher = pattern.matcher(filePath);
                String documentId = matcher.find() ? matcher.group() : UUID.randomUUID().toString();

                // Формируем сообщение для Kafka
                String message = filePath + "|" + userId + "|" + documentId + (date != null ? "|" + date : "");
                documentKafkaTemplate.send("document-processing-queue", message);

                responses.add("Document queued for processing: " + filePath);
                logger.info("File '{}' queued for processing for user: {}", file.getOriginalFilename(), userId);
            } catch (Exception e) {
                logger.error("Failed to process file '{}': {}", file.getOriginalFilename(), e.getMessage());
                responses.add("Failed to process '" + file.getOriginalFilename() + "': " + e.getMessage());
            }
        }

        return responses;
    }
}