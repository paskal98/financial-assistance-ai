package com.microservice.report_service.util;

import com.microservice.report_service.model.dto.ReportRequest;
import com.microservice.report_service.model.dto.TransactionResponse;
import com.microservice.report_service.service.ReportStorageService;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CsvReportGenerator {

    private final ReportStorageService reportStorageService;

    public String generateReport(List<TransactionResponse> transactions, ReportRequest request) {
        String fileName = UUID.randomUUID() + ".csv";

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {

            String[] header = {"Date", "Category", "Type", "Amount", "Description"};
            writer.writeNext(header);

            for (TransactionResponse transaction : transactions) {
                String[] data = {
                        transaction.getDate(),
                        transaction.getCategory(),
                        transaction.getType(),
                        transaction.getAmount().toString(),
                        transaction.getDescription()
                };
                writer.writeNext(data);
            }

            writer.flush();

            return reportStorageService.store(
                    new ByteArrayInputStream(outputStream.toByteArray()),
                    fileName,
                    "text/csv"
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV report: " + e.getMessage(), e);
        }
    }
}