package com.microservice.document_processing_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.document_processing_service.model.dto.DocumentStatusMessageDto;
import com.microservice.document_processing_service.model.entity.Document;
import com.microservice.document_processing_service.service.messaging.WebSocketNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort private int port;

    @Autowired
    private WebSocketNotificationService webSocketNotificationService;


    @Test
    void webSocket_SendsStatusUpdate() throws Exception {
        UUID userId = this.userId;
        BlockingQueue<DocumentStatusMessageDto> messages = new LinkedBlockingQueue<>();

        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer mock-token");

        StompSession session = stompClient.connect(
                "ws://localhost:" + port + "/ws/documents",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        session.subscribe("/topic/documents/" + userId, new StompFrameHandler() {
                            @Override
                            public Type getPayloadType(StompHeaders headers) {
                                return DocumentStatusMessageDto.class;
                            }

                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                messages.add((DocumentStatusMessageDto) payload);
                            }
                        });
                    }
                }
        ).get(5, TimeUnit.SECONDS);

        // Создание документа
        UUID documentId = UUID.randomUUID();
        Document document = new Document();
        document.setId(documentId);
        document.setUserId(userId);
        document.setFilePath("test.pdf");
        document.setStatus("PENDING");
        documentRepository.save(document);

        document.setStatus("PROCESSING");
        documentRepository.save(document);
        webSocketNotificationService.sendStatusUpdate(document);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            DocumentStatusMessageDto message = messages.poll();
            assertNotNull(message, "WebSocket message was not received");
            assertEquals(documentId.toString(), message.getDocumentId());
            assertEquals("PROCESSING", message.getStatus());
        });

        session.disconnect();
    }

}