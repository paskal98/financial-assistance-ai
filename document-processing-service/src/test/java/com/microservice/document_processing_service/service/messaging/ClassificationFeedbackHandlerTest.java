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

public class ClassificationFeedbackHandlerTest {

    @Mock
    private DocumentStateManager documentStateManager;

    @Mock
    private ProcessingStateService processingStateService;

    private ClassificationFeedbackHandler classificationFeedbackHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        classificationFeedbackHandler = new ClassificationFeedbackHandler(documentStateManager, processingStateService);
    }

    @Test
    void handle_ItemsCountFeedback_InitializesState() {
        UUID documentId = UUID.randomUUID();
        FeedbackMessage feedback = new FeedbackMessage(documentId.toString(), "CLASSIFICATION", "ITEMS_COUNT", "5");

        classificationFeedbackHandler.handle(feedback, documentStateManager, processingStateService);

        verify(processingStateService).initializeState(documentId, 5);
        verify(documentStateManager).updateStatus(documentId, "CLASSIFYING");
    }

    @Test
    void handle_FailedFeedback_SetsFailedStatus() {
        UUID documentId = UUID.randomUUID();
        FeedbackMessage feedback = new FeedbackMessage(documentId.toString(), "CLASSIFICATION", "FAILED", "Classification error");

        classificationFeedbackHandler.handle(feedback, documentStateManager, processingStateService);

        verify(documentStateManager).updateStatus(documentId, "FAILED", "Classification error: Classification error");
        verify(processingStateService).clearState(documentId);
    }

    @Test
    void handle_StartedFeedback_UpdatesToStartedClassifying() {
        UUID documentId = UUID.randomUUID();
        FeedbackMessage feedback = new FeedbackMessage(documentId.toString(), "CLASSIFICATION", "STARTED", null);

        classificationFeedbackHandler.handle(feedback, documentStateManager, processingStateService);

        verify(documentStateManager).updateStatus(documentId, "STARTED_CLASSIFYING");
        verifyNoInteractions(processingStateService);
    }

    @Test
    void handle_OtherFeedback_UpdatesStatusAccordingly() {
        UUID documentId = UUID.randomUUID();
        FeedbackMessage feedback = new FeedbackMessage(documentId.toString(), "CLASSIFICATION", "IN_PROGRESS", "3/5 classified");

        classificationFeedbackHandler.handle(feedback, documentStateManager, processingStateService);

        verify(documentStateManager).updateStatus(documentId, "IN_PROGRESS", "3/5 classified");
        verifyNoInteractions(processingStateService);
    }
}
