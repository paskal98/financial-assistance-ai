package com.microservice.document_processing_service.config;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private <K, V> ProducerFactory<K, V> createProducerFactory(Class<?> keySerializer, Class<?> valueSerializer) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    private <K, V> ConsumerFactory<K, V> createConsumerFactory(String groupId, Class<?> keyDeserializer, Class<?> valueDeserializer) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    private ConcurrentKafkaListenerContainerFactory<String, String> createListenerFactory(String groupId) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(createConsumerFactory(groupId, StringDeserializer.class, StringDeserializer.class));
        factory.setConcurrency(3);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(createProducerFactory(StringSerializer.class, StringSerializer.class));
    }

    @Bean
    public KafkaTemplate<String, TransactionItemDto> transactionKafkaTemplate() {
        return new KafkaTemplate<>(createProducerFactory(StringSerializer.class, JsonSerializer.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> feedbackKafkaListenerContainerFactory() {
        return createListenerFactory("doc-feedback-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> ocrFeedbackKafkaListenerContainerFactory() {
        return createListenerFactory("ocr-feedback-group");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> classificationFeedbackKafkaListenerContainerFactory() {
        return createListenerFactory("classification-feedback-group");
    }
}