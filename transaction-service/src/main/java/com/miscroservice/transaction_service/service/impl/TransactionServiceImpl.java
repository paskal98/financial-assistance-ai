package com.miscroservice.transaction_service.service.impl;

import com.miscroservice.transaction_service.exception.AccessDeniedException;
import com.miscroservice.transaction_service.exception.TransactionNotFoundException;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.dto.TransactionStatsResponse;
import com.miscroservice.transaction_service.model.entity.Transaction;
import com.miscroservice.transaction_service.repository.TransactionRepository;
import com.miscroservice.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public TransactionResponse createTransaction(TransactionRequest request, UUID userId) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction.setDate(Instant.parse(request.getDate()));
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction = transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getTransactions(UUID userId, String startDate, String endDate) {
        // Логика фильтрации по датам
        return transactionRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public TransactionResponse updateTransaction(UUID id, TransactionRequest request, UUID userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(TransactionNotFoundException::new);
        if (!transaction.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only update your own transactions");
        }
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction = transactionRepository.save(transaction);
        return mapToResponse(transaction);
    }

    @Override
    public void deleteTransaction(UUID id, UUID userId) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(TransactionNotFoundException::new);
        if (!transaction.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own transactions");
        }
        transactionRepository.delete(transaction);
    }

    @Override
    public TransactionStatsResponse getStats(UUID userId, String startDate, String endDate) {
        // Логика подсчета статистики
        return new TransactionStatsResponse();
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getDate().toString()
        );
    }
}