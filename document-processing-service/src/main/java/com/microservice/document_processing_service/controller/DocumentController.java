package com.microservice.document_processing_service.controller;

import com.microservice.document_processing_service.service.DocumentProcessingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentProcessingService documentProcessingService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<List<String>> uploadDocuments(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "userId") String userId,
            @RequestParam(value = "date", required = false) String date) {

        try {
            List<String> responses = documentProcessingService.processDocuments(files, userId, date);
            return ResponseEntity.accepted().body(responses);
        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for upload request by user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(List.of(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during document upload for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().body(List.of("Internal server error: " + e.getMessage()));
        }
    }
}