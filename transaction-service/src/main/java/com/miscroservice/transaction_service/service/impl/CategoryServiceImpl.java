package com.miscroservice.transaction_service.service.impl;

import com.miscroservice.transaction_service.model.dto.CategoryResponse;
import com.miscroservice.transaction_service.repository.CategoryRepository;
import com.miscroservice.transaction_service.service.CategoryService;
import io.lettuce.core.RedisConnectionException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    private final CategoryRepository categoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CATEGORIES_CACHE_PREFIX = "categories:";

    private boolean cacheFailed = false; // Флаг сбоя кэша

    private boolean cacheFailed() {
        return cacheFailed;
    }

    @Override
    public List<CategoryResponse> getCategories(String type) {
        String cacheKey = CATEGORIES_CACHE_PREFIX + (type != null ? type : "all");

        List<CategoryResponse> cachedCategories = getFromCache(cacheKey);
        boolean cacheAvailable = cachedCategories != null;

        if (cacheAvailable) {
            return cachedCategories;
        }

        List<CategoryResponse> categories;
        try {
            if (type != null) {
                categories = categoryRepository.findAllByType(type).stream()
                        .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getType()))
                        .toList();
            } else {
                categories = categoryRepository.findAll().stream()
                        .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getType()))
                        .toList();
            }
        } catch (Exception e) {
            logger.error("Failed to fetch categories from database for type: {}", type, e);
            return Collections.emptyList();
        }

        if (!categories.isEmpty() && !cacheFailed()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, categories, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                logger.warn("Failed to cache categories for key: {}", cacheKey, e);
            }
        }
        return categories;
    }

    @Retryable(value = RedisConnectionException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private List<CategoryResponse> getFromCache(String cacheKey) {
        try {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData instanceof List<?>) {
                return (List<CategoryResponse>) cachedData;
            }
            return null;
        } catch (Exception e) {
            logger.warn("Failed to retrieve categories from Redis for key: {}. Falling back to database.", cacheKey, e);
            cacheFailed = true;
            return null;
        }
    }


}
