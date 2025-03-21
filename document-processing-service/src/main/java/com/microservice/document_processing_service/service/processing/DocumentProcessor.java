package com.microservice.document_processing_service.service.processing;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import java.util.List;
import java.util.UUID;

public interface DocumentProcessor {
    List<TransactionItemDto> processDocument(String filePath, UUID documentId, UUID userId, String date);
}