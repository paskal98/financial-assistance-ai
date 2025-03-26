package com.microservice.document_processing_service.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservice.document_processing_service.service.processing.DocumentStateManager;
import com.microservice.document_processing_service.service.processing.ProcessingStateService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.shared.dto.FeedbackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackConsumer {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackConsumer.class);

    private final DocumentStateManager documentStateManager;
    private final ProcessingStateService processingStateService;
    private final ObjectMapper objectMapper;
    private final List<FeedbackHandler> handlerList;
    private Map<String, FeedbackHandler> handlers;

    @PostConstruct
    public void init() {
        handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        FeedbackHandler::getStage,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    @KafkaListener(topics = "document-feedback-queue", groupId = "doc-feedback-group",
            containerFactory = "feedbackKafkaListenerContainerFactory")
    public void handleFeedback(String feedbackJson) {
        try {
            FeedbackMessage feedback = objectMapper.readValue(feedbackJson, FeedbackMessage.class);
            FeedbackHandler handler = handlers.get(feedback.getStage());
            if (handler != null) {
                handler.handle(feedback, documentStateManager, processingStateService);
            } else {
                logger.warn("Unknown stage '{}' for document {}", feedback.getStage(), feedback.getDocumentId());
            }
        } catch (Exception e) {
            logger.error("Failed to process feedback: {}", feedbackJson, e);
        }
    }

    @KafkaListener(topics = "document-feedback-queue-dlq", groupId = "doc-feedback-dlq-group",
            containerFactory = "feedbackKafkaListenerContainerFactory")
    public void handleDlqFeedback(String feedbackJson) {
        try {
            FeedbackMessage feedback = objectMapper.readValue(feedbackJson, FeedbackMessage.class);
            UUID documentId = UUID.fromString(feedback.getDocumentId());
            String stage = feedback.getStage();
            String originalStatus = feedback.getStatus();
            String details = feedback.getDetails();

            logger.warn("Received DLQ feedback for document {}: stage={}, status={}, details={}", documentId, stage, originalStatus, details);

            String errorMessage = String.format("Failed to process %s stage after retries: %s (sent to DLQ)", stage, details != null ? details : "Unknown error");
            documentStateManager.updateStatus(documentId, "FAILED", errorMessage);
            processingStateService.clearState(documentId);
        } catch (Exception e) {
            logger.error("Failed to process DLQ feedback: {}", feedbackJson, e);
        }
    }
}