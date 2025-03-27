package com.microservice.report_service.util;

import com.microservice.report_service.model.dto.ReportRequest;
import com.microservice.report_service.model.dto.TransactionResponse;
import com.microservice.report_service.service.ReportStorageService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PdfReportGenerator {

    private final ReportStorageService reportStorageService;

    public String generateReport(List<TransactionResponse> transactions, ReportRequest request) {
        String fileName = UUID.randomUUID() + ".pdf";

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(100, 750);
                content.showText("Financial Report");
                content.endText();

                int y = 700;
                for (TransactionResponse transaction : transactions) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 12);
                    content.newLineAtOffset(100, y);
                    content.showText(transaction.getDate() + " | " + transaction.getCategory() + " | " +
                            transaction.getType() + " | " + transaction.getAmount());
                    content.endText();
                    y -= 20;
                }
            }

            document.save(outputStream);

            return reportStorageService.store(
                    new ByteArrayInputStream(outputStream.toByteArray()),
                    fileName,
                    "application/pdf"
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }
}
