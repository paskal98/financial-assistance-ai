package com.microservice.document_processing_service.service.ai;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import java.util.List;
import java.util.UUID;

public interface AiClassifier {
    List<TransactionItemDto> classifyItems(String ocrText, UUID documentId);
}