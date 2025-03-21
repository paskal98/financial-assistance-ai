package com.microservice.document_processing_service.service.messaging;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DocumentProcessingState {
    private final int totalTransactions;
    private int processedTransactions = 0;
    private boolean failed = false;
    private final StringBuilder errorMessage = new StringBuilder();

    public DocumentProcessingState(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public void incrementProcessed() {
        processedTransactions++;
    }

    public void appendErrorMessage(String message) {
        if (errorMessage.length() > 0) errorMessage.append("; ");
        errorMessage.append(message);
    }

    public boolean isProcessingComplete() {
        return processedTransactions >= totalTransactions;
    }

    public String getErrorMessage() {
        return errorMessage.toString();
    }

}
