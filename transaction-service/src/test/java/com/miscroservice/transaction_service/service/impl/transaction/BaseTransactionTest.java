package com.miscroservice.transaction_service.service.impl.transaction;

import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import com.miscroservice.transaction_service.model.entity.Transaction;
import com.miscroservice.transaction_service.repository.CategoryRepository;
import com.miscroservice.transaction_service.repository.TransactionRepository;
import com.miscroservice.transaction_service.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

abstract class BaseTransactionTest {

    @Mock
    protected TransactionRepository transactionRepository;
    @Mock protected CategoryRepository categoryRepository;
    @Mock protected RedisTemplate<String, Object> redisTemplate;
    @Mock protected KafkaTemplate<String, String> feedbackKafkaTemplate;

    protected TransactionServiceImpl transactionService;
    protected UUID userId;
    protected Transaction transaction;
    protected TransactionRequest transactionRequest;

    protected static final String TRANSACTIONS_CACHE_PREFIX = "transactions:user:";
    protected static final String STATS_CACHE_PREFIX = "stats:user:";

    @BeforeEach
    void baseSetUp() {
        MockitoAnnotations.openMocks(this);
        transactionService = new TransactionServiceImpl(
                transactionRepository,
                categoryRepository,
                redisTemplate,
                feedbackKafkaTemplate
        );
        userId = UUID.randomUUID();
        transaction = new Transaction();
        transactionRequest = new TransactionRequest();

        transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setUserId(userId);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setType("INCOME");
        transaction.setCategory("Salary");
        transaction.setDescription("Monthly salary");
        transaction.setDate(Instant.now());
        transaction.setCreatedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());

        transactionRequest = new TransactionRequest();
        transactionRequest.setAmount(new BigDecimal("100.00"));
        transactionRequest.setType("INCOME");
        transactionRequest.setCategory("Salary");
        transactionRequest.setDescription("Monthly salary");
        transactionRequest.setDate(Instant.now().toString());
    }
}
