package com.microservice.document_processing_service.config;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {
    private static final Logger logger = LoggerFactory.getLogger(StorageConfig.class);

    // Путь к хранилищу из свойств, по умолчанию - временная директория Windows с подпапкой uploads
    @Value("${storage.location:#{systemProperties['java.io.tmpdir'] + '/uploads'}}")
    private String storageLocation;

    // Геттер для использования в других классах (например, DocumentStorageService)
    @Getter
    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            // Формируем путь, совместимый с Windows
            rootLocation = Paths.get(storageLocation);
            Files.createDirectories(rootLocation);
            logger.info("Storage directory initialized at: {}", rootLocation.toAbsolutePath());

            // Проверка доступности директории
            if (!Files.isWritable(rootLocation)) {
                throw new IOException("Storage directory is not writable");
            }
        } catch (IOException e) {
            logger.error("Could not initialize storage directory at: {}", storageLocation, e);
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    // ToDo: Добавить конфигурацию для MinIO/AWS S3
    /*
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }
    */
}