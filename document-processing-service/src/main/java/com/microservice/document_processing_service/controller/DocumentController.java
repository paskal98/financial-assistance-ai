package com.microservice.document_processing_service.controller;

import com.microservice.document_processing_service.model.dto.DocumentUploadRequest;
import com.microservice.document_processing_service.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentStorageService storageService;
    private final KafkaTemplate<String, String> documentKafkaTemplate;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId") String userId,
            @RequestParam(value = "date", required = false) String date) {

                // Сохраняем файл
        String filePath = storageService.store(file);

        String uuidRegex = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
        Pattern pattern = Pattern.compile(uuidRegex);
        Matcher matcher = pattern.matcher(filePath);

        String documentId ;
        // Поиск UUID в строке
        if (matcher.find()) {
            documentId =  matcher.group();
        } else {
            documentId = UUID.randomUUID().toString();
        }

        // Создаем сообщение с путем файла и датой (если есть)
        String message = filePath + "|" + userId + "|" + documentId + (date != null ? "|" + date : "") ;
        documentKafkaTemplate.send("document-processing-queue", message);

        return ResponseEntity.accepted().body("Document queued for processing: " + filePath);
    }
}