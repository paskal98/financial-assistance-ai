package com.microservice.classification_service.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    private static final Logger logger = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        initializeBucket(client);
        return client;
    }

    private void initializeBucket(MinioClient client) {
        try {
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                logger.info("Bucket '{}' created successfully", bucketName);
            } else {
                logger.info("Bucket '{}' already exists", bucketName);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new RuntimeException("Could not initialize MinIO bucket", e);
        }
    }
}