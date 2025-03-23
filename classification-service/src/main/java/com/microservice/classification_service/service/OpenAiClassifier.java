package com.microservice.classification_service.service;

import com.microservice.classification_service.exception.DocumentProcessingException;
import com.microservice.classification_service.model.dto.TransactionItemDto;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpenAiClassifier implements AiClassifier {
    private static final Logger logger = LoggerFactory.getLogger(OpenAiClassifier.class);

    private final OpenAiService openAiService;
    private final PromptBuilder promptBuilder;
    private final ResponseParser responseParser;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${openai.max-tokens:500}")
    private int maxTokens;

    @Override
    @CircuitBreaker(name = "openai-cb", fallbackMethod = "fallbackClassify")
    public List<TransactionItemDto> classifyItems(String ocrText, UUID documentId) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            logger.warn("OCR text is null or empty for document: {}", documentId);
            throw new DocumentProcessingException("OCR text cannot be null or empty for document: " + documentId);
        }

        try {
            logger.info("Classifying items from OCR text for document: {}", documentId);

            String prompt = promptBuilder.buildClassificationPrompt(ocrText);
            ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(new ChatMessage("user", prompt)))
                    .maxTokens(maxTokens)
                    .temperature(0.2)
                    .build();

            String result = openAiService.createChatCompletion(chatRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent()
                    .trim();

            logger.info("OpenAI response for document {}: {}", documentId, result);
            return responseParser.parseResponse(result, documentId);
        } catch (Exception e) {
            logger.error("Failed to classify items with OpenAI for document: {}", documentId, e);
            throw new DocumentProcessingException("Failed to classify items with OpenAI for document: " + documentId, e);
        }
    }

    private List<TransactionItemDto> fallbackClassify(String ocrText, UUID documentId, Throwable t) {
        logger.warn("Fallback triggered for AI classification for document {} due to: {}", documentId, t.getMessage());
        throw new DocumentProcessingException("AI classification failed for document " + documentId + ": " + t.getMessage(), t);
    }
}
