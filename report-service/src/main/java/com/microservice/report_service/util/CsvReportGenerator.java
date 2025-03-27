package com.microservice.report_service.util;

import com.microservice.report_service.model.dto.ReportRequest;
import com.microservice.report_service.model.dto.TransactionResponse;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class CsvReportGenerator {

    public String generateReport(List<TransactionResponse> transactions, ReportRequest request) {
        String fileName = UUID.randomUUID() + ".csv";

        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName))) {
            // Заголовок таблицы
            String[] header = {"Date", "Category", "Type", "Amount", "Description"};
            writer.writeNext(header);

            // Данные транзакций
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

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV report: " + e.getMessage(), e);
        }

        // Вернуть URL сгенерированного файла в файловом хранилище
        return "http://storage-service/reports/" + fileName; // URL в хранилище (MinIO/S3)
    }
}
