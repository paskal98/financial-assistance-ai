package com.microservice.document_processing_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.document_processing_service.model.entity.Document;
import com.microservice.document_processing_service.service.messaging.FeedbackConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FeedbackConsumerIntegrationTest extends BaseIntegrationTest {

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void handleFeedback_UpdatesDocumentStatus() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID userId = this.userId;

        Document document = new Document();
        document.setId(documentId);
        document.setUserId(userId);
        document.setFilePath("test.pdf");
        document.setStatus("PROCESSING");
        document.setCreatedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        documentRepository.save(document);

        String feedbackJson = objectMapper.writeValueAsString(
                new org.shared.dto.FeedbackMessage(documentId.toString(), "OCR",  "PROCESSED", "Text extracted")
        );

        kafkaTemplate.send("document-feedback-queue", feedbackJson);

        await().atMost(5, java.util.concurrent.TimeUnit.SECONDS).until(() ->
                documentRepository.findById(documentId).get().getStatus().equals("PROCESSED")
        );

        Document updatedDoc = documentRepository.findById(documentId).get();
        assertEquals("PROCESSED", updatedDoc.getStatus());
        assertEquals("Text extracted", updatedDoc.getErrorMessage());
    }
}