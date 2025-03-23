package org.shared.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class FeedbackMessage {
    private String documentId;
    private String stage; // "OCR", "CLASSIFICATION", "TRANSACTION"
    private String status; // "STARTED", "FAILED", "SUCCESS", etc.
    private String details;
    private String timestamp;

    public FeedbackMessage(String documentId, String stage, String status, String details) {
        this.documentId = documentId;
        this.stage = stage;
        this.status = status;
        this.details = details;
        this.timestamp = Instant.now().toString();
    }
}
