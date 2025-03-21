package com.microservice.document_processing_service.service;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionProducerService {
    private final KafkaTemplate<String, TransactionItemDto> kafkaTemplate;

    public void sendTransaction(TransactionItemDto item, UUID userId, UUID documentId) {
        Message<TransactionItemDto> message = MessageBuilder
                .withPayload(item)
                .setHeader(KafkaHeaders.TOPIC, "transactions-topic")
                .setHeader("userId", userId.toString())
                .setHeader("documentId", documentId != null ? documentId.toString() : null)
                .build();
        kafkaTemplate.send(message);
    }
}