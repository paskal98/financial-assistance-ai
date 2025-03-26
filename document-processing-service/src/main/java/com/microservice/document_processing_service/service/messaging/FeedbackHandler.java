package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import com.microservice.document_processing_service.service.processing.ProcessingStateService;
import org.shared.dto.FeedbackMessage;

public interface FeedbackHandler {
    void handle(FeedbackMessage feedback, DocumentStateManager stateManager, ProcessingStateService stateService);
    String getStage();
}