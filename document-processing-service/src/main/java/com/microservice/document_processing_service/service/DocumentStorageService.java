package com.microservice.document_processing_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentStorageService {
    String store(MultipartFile file);
}