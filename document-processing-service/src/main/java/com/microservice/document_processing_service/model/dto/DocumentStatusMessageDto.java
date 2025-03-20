package com.microservice.document_processing_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocumentStatusMessageDto {
    private String documentId;
    private String userId;
    private String status;
    private String errorMessage;
}
