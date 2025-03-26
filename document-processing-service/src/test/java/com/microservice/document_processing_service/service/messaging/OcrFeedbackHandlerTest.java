package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.shared.dto.FeedbackMessage;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

public class OcrFeedbackHandlerTest {

    @Mock
    private DocumentStateManager documentStateManager;

    private OcrFeedbackHandler ocrFeedbackHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ocrFeedbackHandler = new OcrFeedbackHandler();
    }

    @Test
    void handle_SuccessfulFeedback_UpdatesStatus() {
        UUID documentId = UUID.randomUUID();
        FeedbackMessage feedback = new FeedbackMessage(documentId.toString(),"OCR",  "PROCESSED", "Text extracted");

        ocrFeedbackHandler.handle(feedback, documentStateManager, null);

        verify(documentStateManager).updateStatus(documentId, "PROCESSED", "Text extracted");
    }

    @Test
    void handle_FailedFeedback_UpdatesStatusWithError() {
        UUID documentId = UUID.randomUUID();
        FeedbackMessage feedback = new FeedbackMessage(documentId.toString(),"OCR",  "FAILED", "OCR engine error");

        ocrFeedbackHandler.handle(feedback, documentStateManager, null);

        verify(documentStateManager).updateStatus(documentId, "FAILED", "OCR engine error");
    }

    @Test
    void getStage_ReturnsCorrectStage() {
        assertEquals("OCR", ocrFeedbackHandler.getStage());
    }
}
