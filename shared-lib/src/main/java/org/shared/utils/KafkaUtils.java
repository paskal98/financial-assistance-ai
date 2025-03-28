// org/shared/utils/KafkaUtils.java
package org.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.shared.dto.FeedbackMessage;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.util.concurrent.CompletableFuture;

public class KafkaUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(KafkaUtils.class);
    private static final String DLQ_SUFFIX = "-dlq";

    /**
     * Sends a message to the specified Kafka topic with delivery confirmation,
     * retry support, and DLQ handling.
     *
     * @param kafkaTemplate The KafkaTemplate instance to use for sending the message.
     * @param topic         The Kafka topic to send the message to.
     * @param message       The message object to send (must be serializable to JSON).
     * @param <T>           The type of the message.
     * @return CompletableFuture<SendResult<String, String>> representing the send operation.
     */
    @Retryable(value = RuntimeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public static <T> CompletableFuture<SendResult<String, String>> sendMessage(
            KafkaTemplate<String, String> kafkaTemplate, String topic, T message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, json);

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Message sent successfully to topic '{}': message={}, offset={}",
                            topic, message, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send message to topic '{}': message={}, error={}",
                            topic, message, ex.getMessage(), ex);
                    sendToDeadLetterQueue(kafkaTemplate, topic, json, message);
                    throw new RuntimeException("Failed to send message to Kafka, sent to DLQ", ex);
                }
            });

            return future;
        } catch (Exception e) {
            logger.error("Error preparing message for topic '{}': message={}, error={}",
                    topic, message, e.getMessage(), e);
            throw new RuntimeException("Failed to prepare and send message", e);
        }
    }

    /**
     * Sends a failed message to the dead letter queue (DLQ).
     *
     * @param kafkaTemplate The KafkaTemplate instance to use.
     * @param originalTopic The original topic the message was sent to.
     * @param json          The serialized message content.
     * @param message       The original message for logging purposes.
     */
    private static <T> void sendToDeadLetterQueue(KafkaTemplate<String, String> kafkaTemplate, String originalTopic,
                                                  String json, T message) {
        String dlqTopic = originalTopic + DLQ_SUFFIX;
        try {
            ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(dlqTopic, json);
            CompletableFuture<SendResult<String, String>> dlqFuture = kafkaTemplate.send(dlqRecord);

            dlqFuture.whenComplete((dlqResult, dlqEx) -> {
                if (dlqEx == null) {
                    logger.info("Message successfully sent to DLQ topic '{}': message={}, offset={}",
                            dlqTopic, message, dlqResult.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send message to DLQ topic '{}': message={}, error={}",
                            dlqTopic, message, dlqEx.getMessage(), dlqEx);
                }
            });
        } catch (Exception e) {
            logger.error("Failed to send message to DLQ topic '{}': message={}, error={}",
                    dlqTopic, message, e.getMessage(), e);
        }
    }

    @Deprecated
    public static CompletableFuture<SendResult<String, String>> sendFeedback(
            KafkaTemplate<String, String> kafkaTemplate, String topic, FeedbackMessage message) {
        return sendMessage(kafkaTemplate, topic, message);
    }
}