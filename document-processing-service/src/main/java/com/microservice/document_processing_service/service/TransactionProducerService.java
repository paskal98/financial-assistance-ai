package com.microservice.document_processing_service.service;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionProducerService {
    private final KafkaTemplate<String, TransactionItemDto> kafkaTemplate;

    public void sendTransaction(TransactionItemDto item) {
        kafkaTemplate.send("transactions-topic", item);
    }
}