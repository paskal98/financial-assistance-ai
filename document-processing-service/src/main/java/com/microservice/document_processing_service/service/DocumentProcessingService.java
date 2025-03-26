package com.microservice.document_processing_service.service;

import com.microservice.document_processing_service.model.dto.DocumentStatusResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface DocumentProcessingService {
    List<String> processDocuments(List<MultipartFile> files, String userId, String date);
    DocumentStatusResponse getDocumentStatus(UUID documentId, String userId);
}