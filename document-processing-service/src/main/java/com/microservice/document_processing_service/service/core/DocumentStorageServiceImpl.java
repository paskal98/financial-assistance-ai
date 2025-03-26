package com.microservice.document_processing_service.service.core;

import com.microservice.document_processing_service.service.DocumentStorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentStorageServiceImpl implements DocumentStorageService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentStorageServiceImpl.class);

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    protected String bucketName;

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

            // Сохраняем файл в MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(uniqueFilename)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            logger.info("File stored successfully in MinIO: {}", uniqueFilename);

            // Возвращаем путь в формате URL или просто имя объекта
            return uniqueFilename; // Или "http://localhost:9000/documents/" + uniqueFilename
        } catch (Exception e) {
            logger.error("Failed to store file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("FailedD to store file in MinIO", e);
        }
    }
}