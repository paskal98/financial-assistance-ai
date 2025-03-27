package com.microservice.report_service.service.impl;

import com.microservice.report_service.exception.ReportGenerationException;
import com.microservice.report_service.model.dto.PagedTransactionResponse;
import com.microservice.report_service.model.dto.ReportRequest;
import com.microservice.report_service.model.dto.ReportResponse;
import com.microservice.report_service.model.dto.TransactionResponse;
import com.microservice.report_service.model.entity.ReportMetadata;
import com.microservice.report_service.repository.ReportMetadataRepository;
import com.microservice.report_service.security.JwtUtil;
import com.microservice.report_service.service.ReportService;
import com.microservice.report_service.service.client.TransactionClient;
import com.microservice.report_service.util.CsvReportGenerator;
import com.microservice.report_service.util.PdfReportGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TransactionClient transactionClient;
    private final ReportMetadataRepository metadataRepository;
    private final PdfReportGenerator pdfReportGenerator;
    private final CsvReportGenerator csvReportGenerator;
    private final JwtUtil jwtUtil;

    @Override
    public ReportResponse generateReport(ReportRequest request, String token) {
        try {
            String jwtToken = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();

            PagedTransactionResponse transactionsResponse = transactionClient.getTransactions(
                    "Bearer " + jwtToken,
                    request.getStartDate().toString(),
                    request.getEndDate().toString(),
                    request.getCategory(),
                    request.getType(),
                    0,
                    1000
            );
            List<TransactionResponse> transactions = transactionsResponse.getContent();

            String fileUrl = switch (request.getFormat().toLowerCase()) {
                case "pdf" -> pdfReportGenerator.generateReport(transactions, request);
                case "csv" -> csvReportGenerator.generateReport(transactions, request);
                default -> throw new ReportGenerationException("Unsupported report format: " + request.getFormat());
            };

            // Rest of the method remains the same
            ReportMetadata metadata = new ReportMetadata();
            metadata.setUserId(UUID.fromString(jwtUtil.extractUserId(token.substring(7))));
            metadata.setFileUrl(fileUrl);
            metadata.setFormat(request.getFormat());
            metadata.setGeneratedAt(Instant.now());
            metadata.setStartDate(request.getStartDate());
            metadata.setEndDate(request.getEndDate());
            metadata.setCategories(request.getCategory()); // Fixed: No need for String.join
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
