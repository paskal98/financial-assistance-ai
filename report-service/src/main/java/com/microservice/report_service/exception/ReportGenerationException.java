package com.microservice.report_service.exception;

public class ReportGenerationException extends RuntimeException {
    public ReportGenerationException(String message) {
        super(message);
    }
}
