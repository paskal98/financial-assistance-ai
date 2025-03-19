package com.miscroservice.transaction_service.repository;

import com.miscroservice.transaction_service.model.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserId(UUID userId);
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
            "AND (:startDate IS NULL OR t.date >= :startDate) " +
            "AND (:endDate IS NULL OR t.date <= :endDate) " +
            "AND (:category IS NULL OR t.category = :category) " +
            "AND (:type IS NULL OR t.type = :type)")
    Page<Transaction> findByFilters(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("category") String category,
            @Param("type") String type,
            Pageable pageable);
}