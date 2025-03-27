package com.microservice.report_service.service.impl;

import com.microservice.report_service.exception.ReportGenerationException;
import com.microservice.report_service.model.dto.*;
import com.microservice.report_service.model.entity.ReportMetadata;
import com.microservice.report_service.model.entity.ReportSummary;
import com.microservice.report_service.repository.ReportMetadataRepository;
import com.microservice.report_service.security.JwtUtil;
import com.microservice.report_service.service.ReportService;
import com.microservice.report_service.service.analytics.ReportAnalyticsService;
import com.microservice.report_service.service.client.TransactionClient;
import com.microservice.report_service.util.CsvReportGenerator;
import com.microservice.report_service.util.PdfReportGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportAnalyticsService analyticsService;
    private final TransactionClient transactionClient;
    private final ReportMetadataRepository metadataRepository;
    private final PdfReportGenerator pdfReportGenerator;
    private final CsvReportGenerator csvReportGenerator;
    private final JwtUtil jwtUtil;

    @Override
    public ReportResponse generateReport(ReportRequest request, String token) {
        try {
            String jwtToken = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();

            // Получение текущих транзакций
            PagedTransactionResponse thisPeriod = transactionClient.getTransactions(
                    "Bearer " + jwtToken,
                    request.getStartDate().toString(),
                    request.getEndDate().toString(),
                    request.getCategory(),
                    request.getType(),
                    0,
                    1000
            );
            List<TransactionResponse> currentTransactions = thisPeriod.getContent();

            // Определим предыдущий период по тем же датам
            Instant start = request.getStartDate();
            Instant end = request.getEndDate();

            long days = java.time.Duration.between(start, end).toDays() + 1;

            Instant prevStart = start.minus(java.time.Duration.ofDays(days));
            Instant prevEnd = end.minus(java.time.Duration.ofDays(days));


            // Получение транзакций за предыдущий период
            PagedTransactionResponse lastPeriod = transactionClient.getTransactions(
                    "Bearer " + jwtToken,
                    prevStart.toString(),
                    prevEnd.toString(),
                    request.getCategory(),
                    request.getType(),
                    0,
                    1000
            );
            List<TransactionResponse> previousTransactions = lastPeriod.getContent();

            ReportSummary summary = analyticsService.analyze(currentTransactions, previousTransactions);

            String fileUrl = switch (request.getFormat().toLowerCase()) {
                case "pdf" -> pdfReportGenerator.generateReport(currentTransactions, request, summary);
                case "csv" -> csvReportGenerator.generateReport(currentTransactions, request);
                default -> throw new ReportGenerationException("Unsupported report format: " + request.getFormat());
            };

            ReportMetadata metadata = new ReportMetadata();
            metadata.setUserId(UUID.fromString(jwtUtil.extractUserId(token.substring(7))));
            metadata.setFileUrl(fileUrl);
            metadata.setFormat(request.getFormat());
            metadata.setGeneratedAt(Instant.now());
            metadata.setStartDate(request.getStartDate());
            metadata.setEndDate(request.getEndDate());
            metadata.setCategories(request.getCategory());
            metadata.setType(request.getType());

            metadataRepository.save(metadata);

            return new ReportResponse(UUID.randomUUID(), fileUrl, "SUCCESS");

        } catch (Exception e) {
            throw new ReportGenerationException("Report generation failed: " + e.getMessage());
        }
    }

    @Override
    public List<ReportResponse> getReportHistory(String token) {
        return metadataRepository.findByUserId(UUID.fromString(jwtUtil.extractUserId(token.substring(7)))).stream()
                .map(metadata -> new ReportResponse(metadata.getId(), metadata.getFileUrl(), "GENERATED"))
                .toList();
    }
}