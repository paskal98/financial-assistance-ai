package com.miscroservice.transaction_service.repository;

import com.miscroservice.transaction_service.model.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByUserId(UUID userId);
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
            "AND (COALESCE(:startDate, t.date) = t.date OR t.date >= :startDate) " +
            "AND (COALESCE(:endDate, t.date) = t.date OR t.date <= :endDate) " +
            "AND (COALESCE(:category, t.category) = t.category) " +
            "AND (COALESCE(:type, t.type) = t.type)")
    Page<Transaction> findByFilters(
            @Param("userId") UUID userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("category") String category,
            @Param("type") String type,
            Pageable pageable);

}