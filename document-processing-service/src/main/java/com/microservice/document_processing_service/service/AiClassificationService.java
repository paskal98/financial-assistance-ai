package com.microservice.document_processing_service.service;

import com.microservice.document_processing_service.exception.DocumentProcessingException;
import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiClassificationService {
    private static final Logger logger = LoggerFactory.getLogger(AiClassificationService.class);
    private final OpenAiService openAiService;

    public List<TransactionItemDto> classifyItems(String ocrText) {
        try {
            // Формируем запрос к OpenAI
            String prompt = "Classify the following items from a receipt into categories (e.g., Food, Transport) and estimate their prices if not provided:\n" + ocrText;
            ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")
                    .messages(List.of(new ChatMessage("user", prompt)))
                    .maxTokens(500)
                    .build();
            String result = openAiService.createChatCompletion(chatRequest).getChoices().get(0).getMessage().getContent();
            logger.info("OpenAI response: {}", result);

            // Пример парсинга ответа (в реальной системе нужно настроить структуру)
            return parseOpenAiResponse(result, ocrText);
        } catch (Exception e) {
            logger.error("Failed to classify items with OpenAI", e);
            throw new DocumentProcessingException("Failed to classify items", e);
        }
    }

    private List<TransactionItemDto> parseOpenAiResponse(String aiResponse, String ocrText) {
        // Заглушка: парсинг ответа OpenAI
        // В реальной системе нужно распарсить структурированный ответ от OpenAI
        List<TransactionItemDto> items = new ArrayList<>();
        TransactionItemDto item = new TransactionItemDto();
        item.setName("Sample Item");
        item.setCategory("Food");
        item.setPrice(BigDecimal.valueOf(10.0));
        item.setVatAmount(BigDecimal.ZERO);
        item.setDate("2025-03-19"); // Можно брать из DocumentUploadRequest
        items.add(item);
        return items;
    }
}