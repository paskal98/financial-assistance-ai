package com.microservice.document_processing_service.controller;

import com.microservice.document_processing_service.model.dto.DocumentStatusResponse;
import com.microservice.document_processing_service.model.entity.Document;
import com.microservice.document_processing_service.repository.DocumentRepository;
import com.microservice.document_processing_service.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentProcessingService documentProcessingService;
    private final DocumentRepository documentRepository;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<List<String>> uploadDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal String userId,
            @RequestParam(value = "date", required = false) String date) {
        UUID.fromString(userId);
        List<String> responses = documentProcessingService.processDocuments(files, userId, date);
        return ResponseEntity.accepted().body(responses);
    }

    @GetMapping("/{documentId}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal String userId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        if (!document.getUserId().toString().equals(userId)) {
            logger.warn("User {} attempted to access document {} owned by another user", userId, documentId);
            return ResponseEntity.status(403).body(new DocumentStatusResponse("FORBIDDEN", "You do not own this document"));
        }
        DocumentStatusResponse response = new DocumentStatusResponse(document.getStatus(), document.getErrorMessage());
        return ResponseEntity.ok(response);
    }
}