package com.microservice.report_service.controller;

import com.microservice.report_service.model.dto.ReportRequest;
import com.microservice.report_service.model.dto.ReportResponse;
import com.microservice.report_service.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    public ResponseEntity<ReportResponse> generateReport(
            @RequestBody ReportRequest request,
            @RequestHeader("Authorization") String token
    ) {
        ReportResponse response = reportService.generateReport(request, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<ReportResponse>> getReportHistory(
            @RequestHeader("Authorization") String token
    ) {
        List<ReportResponse> history = reportService.getReportHistory(token);
        return ResponseEntity.ok(history);
    }
}