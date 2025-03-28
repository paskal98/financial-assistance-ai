package com.microservice.balance_service.service.impl;

import com.microservice.balance_service.model.dto.*;
import com.microservice.balance_service.model.entity.SavingsGoal;
import com.microservice.balance_service.model.entity.UserBalance;
import com.microservice.balance_service.repository.SavingsGoalRepository;
import com.microservice.balance_service.repository.UserBalanceRepository;
import com.microservice.balance_service.service.BalanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceServiceImpl implements BalanceService {
    private static final Logger logger = LoggerFactory.getLogger(BalanceServiceImpl.class);
    private static final String BALANCE_CACHE_PREFIX = "balance:user:";

    private final UserBalanceRepository userBalanceRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final RedisTemplate<String, UserBalanceResponse> balanceRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;


    @Override
    public UserBalanceResponse getUserBalanceResponse(UUID userId) {
        String cacheKey = BALANCE_CACHE_PREFIX + userId;

        UserBalanceResponse cached = balanceRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            logger.info("Balance retrieved from cache for user: {}", userId);
            return cached;
        }

        UserBalance userBalance = userBalanceRepository.findById(userId)
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUserId(userId);
                    newBalance.setBalance(BigDecimal.ZERO);
                    newBalance.setUpdatedAt(Instant.now());
                    return userBalanceRepository.save(newBalance);
                });

        UserBalanceResponse response = new UserBalanceResponse(userBalance.getUserId(), userBalance.getBalance());
        balanceRedisTemplate.opsForValue().set(cacheKey, response, 10, TimeUnit.MINUTES);
        return response;
    }

    @Override
    public SavingsGoalResponse createSavingsGoal(UUID userId, SavingsGoalRequest request) {
        SavingsGoal goal = new SavingsGoal();
        goal.setUserId(userId);
        goal.setName(request.getName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setCreatedAt(Instant.now());
        goal.setUpdatedAt(Instant.now());

        goal = savingsGoalRepository.save(goal);
        return new SavingsGoalResponse(goal.getId(), goal.getName(), goal.getTargetAmount(), goal.getCurrentAmount());
    }

    @Override
    public List<SavingsGoalResponse> getSavingsGoals(UUID userId) {
        return savingsGoalRepository.findByUserId(userId).stream()
                .map(goal -> new SavingsGoalResponse(goal.getId(), goal.getName(), goal.getTargetAmount(), goal.getCurrentAmount()))
                .collect(Collectors.toList());
    }

    @KafkaListener(topics = "balance-update-topic", groupId = "balance-group", containerFactory = "kafkaListenerContainerFactory")
    public void updateBalance(String eventJson) {
        try {
            TransactionEvent event = TransactionEvent.fromJson(eventJson);
            String transactionId = event.getTransactionId();

            if (redisTemplate.opsForValue().get("processed:" + transactionId) != null) {
                logger.info("Transaction {} already processed", transactionId);
                return;
            }

            UUID userId = event.getUserId();
            UserBalance balance = userBalanceRepository.findById(userId)
                    .orElseGet(() -> initializeBalance(userId));

            switch (event.getOperation()) {
                case "CREATE":
                    updateBalanceForCreate(balance, event.getAmount(), event.getType());
                    break;
                case "UPDATE":
                    updateBalanceForUpdate(balance, event.getAmount(), event.getType(),
                            event.getOldAmount(), event.getOldType());
                    break;
                case "DELETE":
                    updateBalanceForDelete(balance, event.getAmount(), event.getType());
                    break;
            }

            userBalanceRepository.save(balance);
            redisTemplate.opsForValue().set("processed:" + transactionId, "true", 1, TimeUnit.HOURS);
            invalidateCache(userId);
        } catch (Exception e) {
            logger.error("Failed to process balance update: {}", eventJson, e);
            // TODO: Отправка в DLQ
        }
    }

    private UserBalance initializeBalance(UUID userId) {
        UserBalance balance = new UserBalance();
        balance.setUserId(userId);
        balance.setBalance(BigDecimal.ZERO);
        balance.setUpdatedAt(Instant.now());
        return balance;
    }

    private void updateBalanceForCreate(UserBalance balance, BigDecimal amount, String type) {
        balance.setBalance(type.equals("INCOME") ?
                balance.getBalance().add(amount) :
                balance.getBalance().subtract(amount));
        balance.setUpdatedAt(Instant.now());
    }

    private void updateBalanceForUpdate(UserBalance balance, BigDecimal newAmount, String newType,
                                        BigDecimal oldAmount, String oldType) {
        BigDecimal oldImpact = oldType.equals("INCOME") ? oldAmount : oldAmount.negate();
        BigDecimal newImpact = newType.equals("INCOME") ? newAmount : newAmount.negate();
        balance.setBalance(balance.getBalance().subtract(oldImpact).add(newImpact));
        balance.setUpdatedAt(Instant.now());
    }

    private void updateBalanceForDelete(UserBalance balance, BigDecimal amount, String type) {
        balance.setBalance(type.equals("INCOME") ?
                balance.getBalance().subtract(amount) :
                balance.getBalance().add(amount));
        balance.setUpdatedAt(Instant.now());
    }

    private void invalidateCache(UUID userId) {
        String cacheKey = BALANCE_CACHE_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }
}