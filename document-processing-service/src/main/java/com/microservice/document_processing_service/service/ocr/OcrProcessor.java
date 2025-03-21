package com.microservice.document_processing_service.service.ocr;

import java.io.InputStream;
import java.util.UUID;

public interface OcrProcessor {
    String extractText(InputStream inputStream, String objectName, UUID documentId, String contentType);
}