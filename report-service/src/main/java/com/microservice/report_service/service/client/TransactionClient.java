package com.microservice.report_service.service.client;

import com.microservice.report_service.model.dto.PagedTransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "transaction-service", url = "${transaction.service.url}")
public interface TransactionClient {
    @GetMapping("/transactions")
    PagedTransactionResponse getTransactions(
            @RequestHeader("Authorization") String token,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "1000") int size
    );
}