package com.miscroservice.transaction_service.service.impl;

import com.miscroservice.transaction_service.exception.AccessDeniedException;
import com.miscroservice.transaction_service.exception.TransactionNotFoundException;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.dto.TransactionStatsResponse;
import com.miscroservice.transaction_service.model.entity.Transaction;
import com.miscroservice.transaction_service.repository.TransactionRepository;
import com.miscroservice.transaction_service.service.TransactionService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TRANSACTIONS_CACHE_PREFIX = "transactions:user:";
    private static final String STATS_CACHE_PREFIX = "stats:user:";

    public TransactionServiceImpl(TransactionRepository transactionRepository, RedisTemplate<String, Object> redisTemplate) {
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
    }

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
        invalidateCache(userId);
        return mapToResponse(transaction);
    }

    @Override
    public List<TransactionResponse> getTransactions(UUID userId, String startDate, String endDate) {
        String cacheKey = TRANSACTIONS_CACHE_PREFIX + userId + ":" + (startDate != null ? startDate : "null") + ":" + (endDate != null ? endDate : "null");
        @SuppressWarnings("unchecked")
        List<TransactionResponse> cachedTransactions = (List<TransactionResponse>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedTransactions != null) {
            return cachedTransactions;
        }

        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        if (startDate != null && endDate != null) {
            Instant start = Instant.parse(startDate);
            Instant end = Instant.parse(endDate);
            transactions = transactions.stream()
                    .filter(t -> !t.getDate().isBefore(start) && !t.getDate().isAfter(end))
                    .toList();
        }

        List<TransactionResponse> response = transactions.stream()
                .map(this::mapToResponse)
                .toList();

        redisTemplate.opsForValue().set(cacheKey, response, 10, TimeUnit.MINUTES);
        return response;
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
        invalidateCache(userId);
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
        invalidateCache(userId);
    }

    @Override
    public TransactionStatsResponse getStats(UUID userId, String startDate, String endDate) {
        String cacheKey = STATS_CACHE_PREFIX + userId + ":" + (startDate != null ? startDate : "null") + ":" + (endDate != null ? endDate : "null");
        TransactionStatsResponse cachedStats = (TransactionStatsResponse) redisTemplate.opsForValue().get(cacheKey);

        if (cachedStats != null) {
            return cachedStats;
        }

        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        if (startDate != null && endDate != null) {
            Instant start = Instant.parse(startDate);
            Instant end = Instant.parse(endDate);
            transactions = transactions.stream()
                    .filter(t -> !t.getDate().isBefore(start) && !t.getDate().isAfter(end))
                    .toList();
        }

        BigDecimal totalIncome = transactions.stream()
                .filter(t -> t.getType().equals("INCOME"))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(t -> t.getType().equals("EXPENSE"))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = transactions.stream()
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        // Добавляем тренды по месяцам
        Map<String, BigDecimal> byMonth = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getDate().atZone(ZoneId.systemDefault()).toLocalDate().getMonth().toString(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));

        TransactionStatsResponse stats = new TransactionStatsResponse();
        stats.setTotalIncome(totalIncome);
        stats.setTotalExpense(totalExpense);
        stats.setByCategory(byCategory);
        stats.setByMonth(byMonth);

        redisTemplate.opsForValue().set(cacheKey, stats, 10, TimeUnit.MINUTES);
        return stats;
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

    private void invalidateCache(UUID userId) {
        String transactionsCacheKey = TRANSACTIONS_CACHE_PREFIX + userId + "*";
        String statsCacheKey = STATS_CACHE_PREFIX + userId + "*";
        redisTemplate.delete(redisTemplate.keys(transactionsCacheKey));
        redisTemplate.delete(redisTemplate.keys(statsCacheKey));
    }
}