package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import com.microservice.document_processing_service.service.processing.ProcessingStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.shared.dto.FeedbackMessage;

import java.util.UUID;

import static org.mockito.Mockito.*;

public class TransactionFeedbackHandlerTest {

    @Mock
    private DocumentStateManager documentStateManager;

    @Mock
    private ProcessingStateService processingStateService;

    private TransactionFeedbackHandler transactionFeedbackHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionFeedbackHandler = new TransactionFeedbackHandler(documentStateManager, processingStateService);
    }

    @Test
    void handle_SuccessFeedback_ProcessingComplete() {
        UUID documentId = UUID.randomUUID();
        FeedbackMessage feedback = new FeedbackMessage(documentId.toString(), "TRANSACTION", "SUCCESS", null);

        when(processingStateService.isProcessingComplete(documentId)).thenReturn(true);

        transactionFeedbackHandler.handle(feedback, documentStateManager, processingStateService);

        verify(processingStateService).incrementProcessed(documentId);
        verify(documentStateManager).updateStatus(documentId, "PROCESSED");
    }

    @Test
    void handle_SuccessFeedback_ProcessingNotComplete() {
        UUID documentId = UUID.randomUUID();
        FeedbackMessage feedback = new FeedbackMessage(documentId.toString(), "TRANSACTION", "SUCCESS", null);

        when(processingStateService.isProcessingComplete(documentId)).thenReturn(false);

        transactionFeedbackHandler.handle(feedback, documentStateManager, processingStateService);

        verify(processingStateService).incrementProcessed(documentId);
        verify(documentStateManager, never()).updateStatus(documentId, "PROCESSED");
    }

    @Test
    void handle_FailedFeedback_SetsFailedStatus() {
        UUID documentId = UUID.randomUUID();
        FeedbackMessage feedback = new FeedbackMessage(documentId.toString(), "TRANSACTION", "FAILED", "Transaction error");

        transactionFeedbackHandler.handle(feedback, documentStateManager, processingStateService);

        verify(documentStateManager).updateStatus(documentId, "FAILED", "Transaction error: Transaction error");
        verify(processingStateService).clearState(documentId);
    }
}
