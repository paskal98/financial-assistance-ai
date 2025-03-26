package com.microservice.document_processing_service.controller;

import com.microservice.document_processing_service.model.dto.DocumentStatusResponse;
import com.microservice.document_processing_service.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
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

    private final DocumentProcessingService documentProcessingService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<List<String>> uploadDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal String userId,
            @RequestParam(value = "date", required = false) String date) {
        List<String> responses = documentProcessingService.processDocuments(files, userId, date);
        return ResponseEntity.accepted().body(responses);
    }

    @GetMapping("/{documentId}/status")
    public ResponseEntity<DocumentStatusResponse> getDocumentStatus(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal String userId) {
        DocumentStatusResponse response = documentProcessingService.getDocumentStatus(documentId, userId);
        return ResponseEntity.ok(response);
    }
}