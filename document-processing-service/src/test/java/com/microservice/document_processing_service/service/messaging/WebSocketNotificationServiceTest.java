package com.microservice.document_processing_service.service.messaging;

import com.microservice.document_processing_service.model.dto.DocumentStatusMessageDto;
import com.microservice.document_processing_service.model.entity.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

public class WebSocketNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketNotificationService webSocketNotificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        webSocketNotificationService = new WebSocketNotificationService(messagingTemplate);
    }

    @Test
    void sendStatusUpdate_SendsMessageToCorrectTopic() {
        UUID userId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        Document document = new Document();
        document.setId(documentId);
        document.setUserId(userId);
        document.setStatus("PROCESSING");

        webSocketNotificationService.sendStatusUpdate(document);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/documents/" + userId),
                argThat((DocumentStatusMessageDto dto) ->
                        dto.getDocumentId().equals(documentId.toString()) &&
                                dto.getStatus().equals("PROCESSING")
                )
        );
    }
}
