package com.microservcie.ocr_service.service;

import java.io.InputStream;
import java.util.UUID;

public interface OcrProcessor {
    String extractText(InputStream inputStream, String objectName, UUID documentId);
}