package com.microservice.document_processing_service.service.event_driven;

import com.microservice.document_processing_service.model.dto.DocumentStatusMessageDto;
import com.microservice.document_processing_service.model.entity.Document;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendStatusUpdate(Document document) {
        DocumentStatusMessageDto message = new DocumentStatusMessageDto(
                document.getId().toString(),
                document.getUserId().toString(),
                document.getStatus(),
                document.getErrorMessage()
        );
        // Отправляем сообщение в топик, специфичный для userId
        messagingTemplate.convertAndSend("/topic/documents/" + document.getUserId(), message);
    }
}
