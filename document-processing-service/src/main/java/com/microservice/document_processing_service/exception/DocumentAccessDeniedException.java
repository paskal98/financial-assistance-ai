package com.microservice.document_processing_service.exception;

public class DocumentAccessDeniedException extends RuntimeException {
    public DocumentAccessDeniedException(String message) {
        super(message);
    }
}