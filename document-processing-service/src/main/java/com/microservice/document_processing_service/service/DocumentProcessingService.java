package com.microservice.document_processing_service.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentProcessingService {
    List<String> processDocuments(List<MultipartFile> files, String userId, String date);
}
