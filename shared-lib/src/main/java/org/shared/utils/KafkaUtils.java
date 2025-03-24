package org.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.shared.dto.FeedbackMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.CompletableFuture;


public class KafkaUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(KafkaUtils.class);
    private static final String DLQ_SUFFIX = "-dlq";

    /**
     * Sends a feedback message to the specified Kafka topic with delivery confirmation,
     * retry support, and DLQ handling. Returns the CompletableFuture for further processing.
     *
     * @param kafkaTemplate The KafkaTemplate instance to use for sending the message.
     * @param topic         The Kafka topic to send the message to.
     * @param message       The FeedbackMessage object to send.
     * @return CompletableFuture<SendResult<String, String>> representing the send operation.
     */
    @Retryable(value = RuntimeException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public static CompletableFuture<SendResult<String, String>> sendFeedback(
            KafkaTemplate<String, String> kafkaTemplate, String topic, FeedbackMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, json);

            // Send the message with a callback
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);

            // Add callback for delivery confirmation
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    // Success case
                    logger.info("Feedback message sent successfully to topic '{}': documentId={}, stage={}, status={}, offset={}",
                            topic, message.getDocumentId(), message.getStage(), message.getStatus(),
                            result.getRecordMetadata().offset());
                } else {
                    // Failure case
                    logger.error("Failed to send feedback message to topic '{}': documentId={}, stage={}, status={}, error={}",
                            topic, message.getDocumentId(), message.getStage(), message.getStatus(), ex.getMessage(), ex);
                    // Send to DLQ
                    sendToDeadLetterQueue(kafkaTemplate, topic, json, message);
                    throw new RuntimeException("Failed to send feedback message to Kafka, sent to DLQ", ex);
                }
            });

            return future;
        } catch (Exception e) {
            logger.error("Error preparing feedback message for topic '{}': documentId={}, error={}",
                    topic, message.getDocumentId(), e.getMessage(), e);
            throw new RuntimeException("Failed to prepare and send feedback message", e);
        }
    }

    /**
     * Sends a failed message to the dead letter queue (DLQ).
     *
     * @param kafkaTemplate The KafkaTemplate instance to use.
     * @param originalTopic The original topic the message was sent to.
     * @param json          The serialized message content.
     * @param message       The original FeedbackMessage for logging purposes.
     */
    private static void sendToDeadLetterQueue(KafkaTemplate<String, String> kafkaTemplate, String originalTopic,
                                              String json, FeedbackMessage message) {
        String dlqTopic = originalTopic + DLQ_SUFFIX;
        try {
            ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(dlqTopic, json);
            CompletableFuture<SendResult<String, String>> dlqFuture = kafkaTemplate.send(dlqRecord);

            dlqFuture.whenComplete((dlqResult, dlqEx) -> {
                if (dlqEx == null) {
                    logger.info("Message successfully sent to DLQ topic '{}': documentId={}, offset={}",
                            dlqTopic, message.getDocumentId(), dlqResult.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to send message to DLQ topic '{}': documentId={}, error={}",
                            dlqTopic, message.getDocumentId(), dlqEx.getMessage(), dlqEx);
                }
            });
        } catch (Exception e) {
            logger.error("Failed to send message to DLQ topic '{}': documentId={}, error={}",
                    dlqTopic, message.getDocumentId(), e.getMessage(), e);
        }
    }
}