package com.microservcie.ocr_service.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class OcrResultMessage {
    private String ocrText; // Теперь может быть сжатым и закодированным в Base64
    private UUID documentId;
    private UUID userId;
    private String date;
    private boolean isCompressed = false; // Флаг, сжат ли текст
}