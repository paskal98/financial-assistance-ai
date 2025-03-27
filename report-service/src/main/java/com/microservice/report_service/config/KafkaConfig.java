package com.microservice.report_service.config;

import com.microservice.report_service.model.dto.ReportRequest;
import com.microservice.report_service.model.dto.ReportResponse;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Настройка ConsumerFactory для ReportRequest
    @Bean
    public ConsumerFactory<String, ReportRequest> reportRequestConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "report-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Настройка JsonDeserializer для ReportRequest
        JsonDeserializer<ReportRequest> deserializer = new JsonDeserializer<>(ReportRequest.class, false);
        deserializer.addTrustedPackages("com.miscroservice.report_service.model.dto");

        return new DefaultKafkaConsumerFactory<>(configProps, new StringDeserializer(), deserializer);
    }

    // Настройка Listener Container Factory для обработки ReportRequest
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReportRequest> reportRequestListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ReportRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reportRequestConsumerFactory());
        factory.setConcurrency(3); // Параллельная обработка (можно настроить под нагрузку)
        return factory;
    }

    // Настройка ProducerFactory для ReportResponse
    @Bean
    public ProducerFactory<String, ReportResponse> reportResponseProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Повторные попытки при сбоях
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Ждать подтверждения от всех реплик

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    // KafkaTemplate для отправки ReportResponse
    @Bean
    public KafkaTemplate<String, ReportResponse> reportResponseKafkaTemplate() {
        return new KafkaTemplate<>(reportResponseProducerFactory());
    }

    // Настройка ProducerFactory для отправки запросов (опционально, если нужно отправлять вручную)
    @Bean
    public ProducerFactory<String, ReportRequest> reportRequestProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, ReportRequest> reportRequestKafkaTemplate() {
        return new KafkaTemplate<>(reportRequestProducerFactory());
    }
}