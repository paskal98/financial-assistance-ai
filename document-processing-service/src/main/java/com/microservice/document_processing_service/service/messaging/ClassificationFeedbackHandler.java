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
public class ClassificationFeedbackHandler implements FeedbackHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClassificationFeedbackHandler.class);
    private static final String STAGE = "CLASSIFICATION";

    private final DocumentStateManager documentStateManager;
    private final ProcessingStateService processingStateService;

    @Override
    public void handle(FeedbackMessage feedback, DocumentStateManager stateManager, ProcessingStateService stateService) {
        UUID documentId = UUID.fromString(feedback.getDocumentId());
        String status = feedback.getStatus();
        String details = feedback.getDetails();

        switch (status) {
            case "ITEMS_COUNT":
                int totalItems = Integer.parseInt(details);
                processingStateService.initializeState(documentId, totalItems);
                documentStateManager.updateStatus(documentId, "CLASSIFYING");
                logger.info("Initialized classification for document {} with {} items", documentId, totalItems);
                break;
            case "FAILED":
                documentStateManager.updateStatus(documentId, "FAILED", "Classification error: " + details);
                processingStateService.clearState(documentId);
                logger.warn("Classification failed for document {}: {}", documentId, details);
                break;
            case "STARTED":
                documentStateManager.updateStatus(documentId, "STARTED_CLASSIFYING");
                logger.info("Started classification for document {}", documentId);
                break;
            default:
                documentStateManager.updateStatus(documentId, status, details);
                logger.info("Processed classification feedback for document {}: status={}, details={}", documentId, status, details);
        }
    }

    @Override
    public String getStage() {
        return STAGE;
    }
}