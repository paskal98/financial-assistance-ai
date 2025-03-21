package com.microservice.document_processing_service.service.ocr;

import java.util.UUID;

public interface OcrProcessor {
    String extractTextFromImage(String objectName, UUID documentId);
}