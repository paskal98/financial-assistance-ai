// org/shared/dto/TransactionEvent.java
package org.shared.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransactionEvent {
    private String transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String type;
    private String operation; // "CREATE", "UPDATE", "DELETE"
    private BigDecimal oldAmount; // Для UPDATE
    private String oldType; // Для UPDATE

    public TransactionEvent(String transactionId, UUID userId, BigDecimal amount, String type, String operation) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.operation = operation;
    }

    public TransactionEvent(String transactionId, UUID userId, BigDecimal amount, String type, String operation,
                            BigDecimal oldAmount, String oldType) {
        this(transactionId, userId, amount, type, operation);
        this.oldAmount = oldAmount;
        this.oldType = oldType;
    }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize TransactionEvent", e);
        }
    }

    public static TransactionEvent fromJson(String json) {
        try {
            return new ObjectMapper().readValue(json, TransactionEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize TransactionEvent", e);
        }
    }
}