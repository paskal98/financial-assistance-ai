package com.miscroservice.transaction_service.service;

import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.dto.TransactionStatsResponse;

import java.util.List;
import java.util.UUID;

public interface TransactionService {
    TransactionResponse createTransaction(TransactionRequest request, UUID userId);
    List<TransactionResponse> getTransactions(UUID userId, String startDate, String endDate);
    TransactionResponse updateTransaction(UUID id, TransactionRequest request, UUID userId);
    void deleteTransaction(UUID id, UUID userId);
    TransactionStatsResponse getStats(UUID userId, String startDate, String endDate);
}