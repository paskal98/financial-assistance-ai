package com.microservice.classification_service.service;

import com.microservice.classification_service.model.dto.TransactionItemDto;

import java.util.List;
import java.util.UUID;

public interface AiClassifier {
    List<TransactionItemDto> classifyItems(String ocrText, UUID documentId);
}