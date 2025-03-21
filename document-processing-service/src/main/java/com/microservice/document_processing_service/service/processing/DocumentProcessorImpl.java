package com.microservice.document_processing_service.service.processing;

import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import com.microservice.document_processing_service.service.ai.OpenAiClassifier;
import com.microservice.document_processing_service.service.ocr.TesseractOcrProcessor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentProcessorImpl implements DocumentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessorImpl.class);

    private final TesseractOcrProcessor tesseractOcrProcessor;
    private final OpenAiClassifier openAiClassifier;
    private final DocumentStateManager documentStateManager;

    @Override
    public List<TransactionItemDto> processDocument(String filePath, UUID documentId, UUID userId, String date) {
        logger.info("Processing document: {} for documentId: {}", filePath, documentId);

        documentStateManager.updateStatus(documentId, "EXTRACTING_TEXT");
        String ocrText = tesseractOcrProcessor.extractTextFromImage(filePath, documentId);

        documentStateManager.updateStatus(documentId, "CLASSIFYING");
        List<TransactionItemDto> items = openAiClassifier.classifyItems(ocrText, documentId);

        if (date != null) {
            Instant parsedDate = Instant.parse(date);
            items.forEach(item -> item.setDate(parsedDate));
        }

        logger.info("Processed {} items for document: {}", items.size(), documentId);
        return items;
    }
}