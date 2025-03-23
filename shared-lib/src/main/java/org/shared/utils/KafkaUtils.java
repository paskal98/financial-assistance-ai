package org.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shared.dto.FeedbackMessage;
import org.springframework.kafka.core.KafkaTemplate;

public class KafkaUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void sendFeedback(KafkaTemplate<String, String> kafkaTemplate, String topic, FeedbackMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send feedback message", e);
        }
    }
}