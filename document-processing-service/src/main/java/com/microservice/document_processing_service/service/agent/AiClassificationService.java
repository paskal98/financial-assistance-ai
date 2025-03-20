package com.microservice.document_processing_service.service.agent;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microservice.document_processing_service.exception.DocumentProcessingException;
import com.microservice.document_processing_service.model.dto.TransactionItemDto;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
public class AiClassificationService {
    private static final Logger logger = LoggerFactory.getLogger(AiClassificationService.class);

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper = new ObjectMapper(); // For JSON parsing

    @Value("${openai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.max-tokens:500}")
    private int maxTokens;


    public List<TransactionItemDto> classifyItems(String ocrText) {
        if (ocrText == null || ocrText.trim().isEmpty()) {
            logger.warn("OCR text is null or empty");
            throw new DocumentProcessingException("OCR text cannot be null or empty");
        }

        try {
            logger.info("Classifying items from OCR text: {}", ocrText);

            // Build a structured prompt for consistent JSON output
            String prompt = buildClassificationPrompt(ocrText);
            ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(List.of(new ChatMessage("user", prompt)))
                    .maxTokens(maxTokens)
                    .temperature(0.2) // Lower temperature for more deterministic output
                    .build();

            // Call OpenAI API
            String result = openAiService.createChatCompletion(chatRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent()
                    .trim();

            logger.info("OpenAI response: {}", result);

            // Parse the response into TransactionItemDto list
            return parseOpenAiResponse(result);
        } catch (Exception e) {
            logger.error("Failed to classify items with OpenAI for OCR text: {}", ocrText, e);
            throw new DocumentProcessingException("Failed to classify items with OpenAI", e);
        }
    }

    private String buildClassificationPrompt(String ocrText) {
        return """
            You are an expert in receipt analysis. Given the following text extracted from a receipt, classify each item into a category (choose from the provided list) and extract the price, date, description, payment method. If any field is missing, estimate it reasonably or set it to null. Return the result as a JSON array of objects with fields: "name", "category", "price", "date", "description", "paymentMethod". Ensure the output is valid JSON.

            Categories:
            - Groceries
            - Restaurants
            - Transport
            - Entertainment
            - Utilities
            - Shopping
            - Healthcare
            - Education
            - Travel
            - Subscriptions
            - Rent
            - Taxes
            - Gifts Given
            - Pets
            - Hobbies
            - Insurance
            - Repairs
            - Business Expenses
            - Childcare
            - Debt Repayment
            - Salary
            - Freelance
            - Investments
            - Bonuses
            - Gifts Received
            - Refunds
            - Passive Income
            - Sales
            - Grants
            - Cashback
            
            Type of it:
            - EXPENSE
            - INCOME

            Receipt text:
            %s

            Example output:
            [
                {"name": "Milk", "type": "EXPENSE", "category": "Groceries", "price": 2.50, "date": "2024-03-20T12:00:00Z", "description": "Organic milk", "paymentMethod": "Credit Card" },
                {"name": "Bus Ticket", "type": "EXPENSE", "category": "Transport", "price": 1.20, "date": "2024-03-20T12:30:00Z", "description": "City bus", "paymentMethod": "Cash"}
            ]
            """.formatted(ocrText);
    }


    private List<TransactionItemDto> parseOpenAiResponse(String aiResponse) {
        this.objectMapper.registerModule(new JavaTimeModule());
        try {
            // Парсим JSON в список DTO
            List<TransactionItemDto> items = objectMapper.readValue(aiResponse, new TypeReference<>() {});

            // Валидация и исправление данных
            for (TransactionItemDto item : items) {
                if (item.getName() == null || item.getName().trim().isEmpty()) {
                    item.setName("Unknown Item");
                }
                if (item.getCategory() == null || item.getCategory().trim().isEmpty()) {
                    item.setCategory("Unknown");
                }
                if (item.getType() == null || item.getType().trim().isEmpty()) {
                    item.setCategory("UNKNOWN");
                }
                if (item.getPrice() == null) {
                    item.setPrice(BigDecimal.ZERO);
                }
                if (item.getDate() == null) {
                    item.setDate(Instant.now());
                }
                if (item.getDescription() == null) {
                    item.setDescription("No description");
                }
                if (item.getPaymentMethod() == null) {
                    item.setPaymentMethod("Unknown");
                }
                if (item.getDocumentId() == null) {
                    item.setDocumentId(UUID.randomUUID());
                }
            }

            logger.info("Parsed {} items from OpenAI response", items.size());
            return items;
        } catch (Exception e) {
            logger.error("Failed to parse OpenAI response: {}", aiResponse, e);
            throw new DocumentProcessingException("Invalid OpenAI response format", e);
        }
    }


}