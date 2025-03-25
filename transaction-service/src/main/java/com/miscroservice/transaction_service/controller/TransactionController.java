package com.miscroservice.transaction_service.controller;

import com.miscroservice.transaction_service.model.dto.TransactionRequest;
import com.miscroservice.transaction_service.model.dto.TransactionResponse;
import com.miscroservice.transaction_service.model.dto.TransactionStatsResponse;
import com.miscroservice.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;


    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal String userId,
            BindingResult bindingResult) {
        TransactionResponse response = transactionService.createTransaction(request, UUID.fromString(userId), bindingResult);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal String userId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransactionResponse> transactions = transactionService.getTransactions(
                UUID.fromString(userId), startDate, endDate, category, type, pageable);
        return ResponseEntity.ok(transactions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal String userId,
            BindingResult bindingResult) {
        TransactionResponse response = transactionService.updateTransaction(id, request, UUID.fromString(userId), bindingResult);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        transactionService.deleteTransaction(id, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<TransactionStatsResponse> getStats(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        TransactionStatsResponse stats = transactionService.getStats(UUID.fromString(userId), startDate, endDate);
        return ResponseEntity.ok(stats);
    }
}