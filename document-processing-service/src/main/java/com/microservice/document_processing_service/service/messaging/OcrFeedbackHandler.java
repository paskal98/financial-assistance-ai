package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import com.microservice.document_processing_service.service.processing.ProcessingStateService;
import lombok.RequiredArgsConstructor;
import org.shared.dto.FeedbackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OcrFeedbackHandler implements FeedbackHandler {
    private static final Logger logger = LoggerFactory.getLogger(OcrFeedbackHandler.class);
    private static final String STAGE = "OCR";

    @Override
    public void handle(FeedbackMessage feedback, DocumentStateManager stateManager, ProcessingStateService stateService) {
        UUID documentId = UUID.fromString(feedback.getDocumentId());
        stateManager.updateStatus(documentId, feedback.getStatus(), feedback.getDetails());
        logger.info("Processed OCR feedback for document {}: status={}, details={}", documentId, feedback.getStatus(), feedback.getDetails());
    }

    @Override
    public String getStage() {
        return STAGE;
    }
}