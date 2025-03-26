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
public class TransactionFeedbackHandler implements FeedbackHandler {
    private static final Logger logger = LoggerFactory.getLogger(TransactionFeedbackHandler.class);
    private static final String STAGE = "TRANSACTION";

    private final DocumentStateManager documentStateManager;
    private final ProcessingStateService processingStateService;

    @Override
    public void handle(FeedbackMessage feedback, DocumentStateManager stateManager, ProcessingStateService stateService) {
        UUID documentId = UUID.fromString(feedback.getDocumentId());
        String status = feedback.getStatus();
        String details = feedback.getDetails();

        if ("SUCCESS".equals(status)) {
            processingStateService.incrementProcessed(documentId);
            if (processingStateService.isProcessingComplete(documentId)) {
                documentStateManager.updateStatus(documentId, "PROCESSED");
                logger.info("Transaction processing completed for document {}", documentId);
            } else {
                logger.info("Processed transaction for document {}", documentId);
            }
        } else if ("FAILED".equals(status)) {
            documentStateManager.updateStatus(documentId, "FAILED", "Transaction error: " + details);
            processingStateService.clearState(documentId);
            logger.warn("Transaction failed for document {}: {}", documentId, details);
        }
    }

    @Override
    public String getStage() {
        return STAGE;
    }
}