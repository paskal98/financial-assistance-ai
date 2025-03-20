package com.microservice.document_processing_service.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
public class Document {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String status; // PENDING, PROCESSING, EXTRACTING_TEXT, CLASSIFYING, PROCESSED, FAILED

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}