package com.miscroservice.transaction_service.service.impl;

import com.miscroservice.transaction_service.exception.AccessDeniedException;
import com.miscroservice.transaction_service.exception.TransactionNotFoundException;
import com.miscroservice.transaction_service.model.dto.TransactionItemDto;
import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.dto.TransactionStatsResponse;
import com.miscroservice.transaction_service.model.entity.Transaction;
import com.miscroservice.transaction_service.repository.CategoryRepository;
import com.miscroservice.transaction_service.repository.TransactionRepository;
import com.miscroservice.transaction_service.service.TransactionService;
import io.lettuce.core.RedisConnectionException;
import lombok.RequiredArgsConstructor;
import org.shared.dto.FeedbackMessage;
import org.shared.utils.KafkaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private static final String FEEDBACK_TOPIC = "document-feedback-queue";

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final KafkaTemplate<String, String> feedbackKafkaTemplate;

    private static final String TRANSACTIONS_CACHE_PREFIX = "transactions:user:";
    private static final String STATS_CACHE_PREFIX = "stats:user:";

    @Override
    public TransactionResponse createTransaction(TransactionRequest request, UUID userId) {
        validateCategory(request.getCategory());
        validateType(request.getType());
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction.setDate(Instant.parse(request.getDate()));
        transaction.setPaymentMethod(request.getPaymentMethod());
        transaction.setCreatedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());

        try {
            transaction = transactionRepository.save(transaction);
        } catch (Exception e) {
            logger.error("Failed to save transaction for user: {}", userId, e);
            throw new RuntimeException("Failed to create transaction", e);
        }

        invalidateCache(userId);
        return mapToResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getTransactions(
            UUID userId, String startDate, String endDate, String category, String type, Pageable pageable) {
        String cacheKey = TRANSACTIONS_CACHE_PREFIX + userId + ":" + startDate + ":" + endDate + ":" +
                category + ":" + type + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();
        @SuppressWarnings("unchecked")
        Page<TransactionResponse> cachedTransactions = (Page<TransactionResponse>) redisTemplate.opsForValue().get(cacheKey);

        if (cachedTransactions != null) {
            return cachedTransactions;
        }

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;
        Page<Transaction> transactions = transactionRepository.findByFilters(userId, start, end, category, type, pageable);
        Page<TransactionResponse> response = transactions.map(this::mapToResponse);

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
        validateCategory(request.getCategory());
        validateType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction.setUpdatedAt(Instant.now());
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

    @Override
    public void processTransactionFromDocument(TransactionItemDto item, UUID userId, UUID documentId) {
        logger.info("Processing transaction from document: {} for user: {}", item, userId);

        try {
            validateCategory(item.getCategory());
            Transaction transaction = mapToTransaction(item, userId, documentId);
            transactionRepository.save(transaction);
            logger.info("Transaction saved from document: {}", transaction);

            FeedbackMessage successFeedback = new FeedbackMessage(documentId.toString(), "TRANSACTION", "SUCCESS", item.getName());
            sendFeedbackAndHandle(feedbackKafkaTemplate, FEEDBACK_TOPIC, successFeedback, documentId, "success");
            invalidateCache(userId);
        } catch (Exception e) {
            logger.error("Failed to process transaction from document: {}", item, e);
            FeedbackMessage failureFeedback = new FeedbackMessage(documentId.toString(), "TRANSACTION", "FAILED", e.getMessage());
            sendFeedbackAndHandle(feedbackKafkaTemplate, FEEDBACK_TOPIC, failureFeedback, documentId, "failure");
            throw e;
        }
    }

    @KafkaListener(topics = "transactions-topic", groupId = "transaction-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeTransactionFromDocument(TransactionItemDto item) {
        UUID userId = item.getUserId();
        if (userId == null) {
            logger.error("userId is missing in TransactionItemDto: {}", item);
            throw new IllegalArgumentException("userId is required for transaction processing");
        }
        UUID documentId = item.getDocumentId();
        processTransactionFromDocument(item, userId, documentId);
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

    private Transaction mapToTransaction(TransactionItemDto item, UUID userId, UUID documentId) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setAmount(item.getPrice());
        transaction.setType(item.getType());
        transaction.setCategory(item.getCategory());
        transaction.setDescription(item.getName());
        transaction.setDate(item.getDate());
        transaction.setPaymentMethod(item.getPaymentMethod());
        transaction.setDocumentId(documentId);
        transaction.setCreatedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());
        return transaction;
    }

    @Retryable(value = RedisConnectionException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private Set<String> fetchKeys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    private void invalidateCache(UUID userId) {
        Set<String> keysToDelete = new HashSet<>();
        boolean transactionKeysFetched = false;
        boolean statsKeysFetched = false;

        try {
            keysToDelete.addAll(fetchKeys(TRANSACTIONS_CACHE_PREFIX + userId + "*"));
            transactionKeysFetched = true;
        } catch (Exception e) {
            logger.warn("Failed to fetch transaction cache keys for user: {}", userId, e);
        }

        try {
            keysToDelete.addAll(fetchKeys(STATS_CACHE_PREFIX + userId + "*"));
            statsKeysFetched = true;
        } catch (Exception e) {
            logger.warn("Failed to fetch stats cache keys for user: {}", userId, e);
        }

        if (!keysToDelete.isEmpty() && (transactionKeysFetched || statsKeysFetched)) {
            try {
                redisTemplate.delete(keysToDelete);
            } catch (Exception e) {
                logger.warn("Failed to delete cache keys for user: {}", userId, e);
            }
        }
    }

    private void validateCategory(String categoryName) {
        if (!categoryRepository.findByName(categoryName).isPresent()) {
            throw new IllegalArgumentException("Category '" + categoryName + "' does not exist");
        }
    }

    private void validateType(String type) {
        if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
            throw new IllegalArgumentException("Type must be either 'INCOME' or 'EXPENSE'");
        }
    }

    private void sendFeedbackAndHandle(KafkaTemplate<String, String> kafkaTemplate, String topic,
                                       FeedbackMessage message, UUID documentId, String phase) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                CompletableFuture<SendResult<String, String>> future = KafkaUtils.sendFeedback(kafkaTemplate, topic, message);
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send {} feedback for document: {}", phase, documentId, ex);
                    }
                });
                break;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    logger.error("Failed to send {} feedback after {} attempts for document: {}", phase, maxAttempts, documentId, e);
                }
                try {
                    Thread.sleep(1000 * attempt); // Backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}