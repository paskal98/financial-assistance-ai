package com.miscroservice.transaction_service.service;

import com.miscroservice.transaction_service.model.dto.TransactionItemDto;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.dto.TransactionStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.BindingResult;

import java.time.Instant;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse createTransaction(TransactionRequest request, UUID userId, BindingResult bindingResult);

    Page<TransactionResponse> getTransactions(UUID userId, Instant startDate, Instant endDate, String category, String type, Pageable pageable);

    TransactionResponse updateTransaction(UUID id, TransactionRequest request, UUID userId, BindingResult bindingResult);

    void deleteTransaction(UUID id, UUID userId);

    TransactionStatsResponse getStats(UUID userId, String startDate, String endDate);

    void processTransactionFromDocument(TransactionItemDto item, UUID userId, UUID documentId);
}