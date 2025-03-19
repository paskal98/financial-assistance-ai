package com.microservice.document_processing_service.service.impl;

import com.microservice.document_processing_service.config.StorageConfig;
import com.microservice.document_processing_service.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentStorageServiceImpl implements DocumentStorageService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentStorageServiceImpl.class);

    private final StorageConfig storageConfig;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            logger.warn("Attempted to store an empty or null file");
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        try {
            // Генерируем уникальное имя файла
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".unknown";
            String uniqueFilename = UUID.randomUUID() + extension;
            Path destinationFile = storageConfig.getRootLocation().resolve(uniqueFilename);

            // Сохраняем файл
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("File stored successfully: {}", destinationFile.toAbsolutePath());

            // ToDo: Добавить поддержку MinIO/AWS S3
            // Например:
            // minioClient.putObject(PutObjectArgs.builder()
            //     .bucket("documents")
            //     .object(uniqueFilename)
            //     .stream(file.getInputStream(), file.getSize(), -1)
            //     .build());
            // return "s3://documents/" + uniqueFilename;

            return destinationFile.toString();
        } catch (IOException e) {
            logger.error("Failed to store file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to store file", e);
        }
    }
}