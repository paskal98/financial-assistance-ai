package com.microservice.report_service.util;

import com.microservice.report_service.model.dto.ReportRequest;
import com.microservice.report_service.model.dto.TransactionResponse;
import com.microservice.report_service.model.entity.ReportSummary;
import com.microservice.report_service.service.ReportStorageService;
import com.microservice.report_service.service.analytics.ReportAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PdfReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PdfReportGenerator.class);
    private static final int PAGE_HEIGHT = 750;

    private final ReportStorageService reportStorageService;
    private final ReportAnalyticsService analyticsService;

    public String generateReport(List<TransactionResponse> transactions, ReportRequest request, ReportSummary summary) {
        String fileName = UUID.randomUUID() + ".pdf";

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);

            drawCenteredText(document, content, "Financial Report", 780, 18, Color.BLACK);

            // Графики
            BufferedImage pieChartImage = generateCategoryPieChart(transactions);
            BufferedImage lineChartImage = generateDateLineChart(transactions);
            insertImage(document, content, pieChartImage, 50, 470, 240, 240);
            insertImage(document, content, lineChartImage, 310, 470, 240, 240);

            // Аналитика
            int y = 410;
            drawSectionText(content, "Total Expenses: " + summary.getTotalAmount() + " UAH", 50, y, 12, Color.BLACK);
            drawSectionText(content, "Average per Day: " + summary.getAveragePerDay() + " UAH", 50, y - 20, 12, Color.DARK_GRAY);
            drawSectionText(content, "Top Category: " + summary.getTopCategory(), 50, y - 40, 12, new Color(34, 139, 34));
            drawSectionText(content, "Top Spending Day: " + summary.getTopDay(), 50, y - 60, 12, new Color(70, 130, 180));
            drawSectionText(content, "Active Days: " + summary.getActiveDaysCount(), 50, y - 80, 12, Color.GRAY);
            drawSectionText(content, "Unique Categories: " + summary.getUniqueCategoriesCount(), 50, y - 100, 12, Color.GRAY);

            y=280;
            drawSectionText(content, "Comparison with Previous Period:", 50, y, 13, Color.BLACK);
            drawSectionText(content, "Total This Period: " + summary.getTotalAmount() + " UAH", 50, y - 20, 12, Color.BLACK);
            drawSectionText(content, "Total Last Period: " + summary.getPreviousTotalAmount() + " UAH", 50, y - 40, 12, Color.GRAY);
            drawSectionText(content, "Change: " + summary.getDeltaPercentage() + "%", 50, y - 60, 12,
                    summary.getDeltaPercentage().compareTo(BigDecimal.ZERO) >= 0 ? new Color(34, 139, 34) : Color.RED);
            drawSectionText(content, "Avg/Day This Period: " + summary.getAveragePerDay() + " UAH", 300, y - 20, 12, Color.BLACK);
            drawSectionText(content, "Avg/Day Last Period: " + summary.getPreviousAveragePerDay() + " UAH", 300, y - 40, 12, Color.GRAY);


            int legendY = y - 100;
            drawSectionText(content, "Category Color Legend:", 50, legendY, 12, Color.DARK_GRAY);
            drawSectionText(content, "Red = High Cost (> 15,000)", 50, legendY - 20, 11, Color.RED);
            drawSectionText(content, "Green = Mid Range (5,000–15,000)", 50, legendY - 40, 11, new Color(0, 128, 0));
            drawSectionText(content, "Blue = Daily (< 5,000)", 50, legendY - 60, 11, new Color(30, 144, 255));

            int overviewY = 140;
            LocalDate now = LocalDate.now();

            BigDecimal todayTotal = analyticsService.getTotalForPeriod(transactions, now, now);
            BigDecimal yesterdayTotal = analyticsService.getTotalForPeriod(transactions, now.minusDays(1), now.minusDays(1));
            BigDecimal weekTotal = analyticsService.getTotalForPeriod(transactions, now.minusDays(6), now);
            BigDecimal monthTotal = analyticsService.getTotalForPeriod(transactions, now.withDayOfMonth(1), now);

            BufferedImage chartToday = generateMiniPie("Today", todayTotal);
            BufferedImage chartYday = generateMiniPie("Yesterday", yesterdayTotal);
            BufferedImage chartWeek = generateMiniPie("Last 7 Days", weekTotal);
            BufferedImage chartMonth = generateMiniPie("This Month", monthTotal);

            insertImage(document, content, chartToday, 50, overviewY, 120, 120);
            insertImage(document, content, chartYday, 180, overviewY, 120, 120);
            insertImage(document, content, chartWeek, 310, overviewY, 120, 120);
            insertImage(document, content, chartMonth, 440, overviewY, 120, 120);

            content.close();
            document.save(outputStream);

            return reportStorageService.store(
                    new ByteArrayInputStream(outputStream.toByteArray()),
                    fileName,
                    "application/pdf"
            );

        } catch (Exception e) {
            logger.error("Failed to generate PDF report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void drawCenteredText(PDDocument document, PDPageContentStream content, String text, int y, int fontSize, Color color) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, fontSize);
        content.setNonStrokingColor(color);
        float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(text) / 1000 * fontSize;
        float pageWidth = PDRectangle.A4.getWidth();
        float startX = (pageWidth - titleWidth) / 2;
        content.newLineAtOffset(startX, y);
        content.showText(text);
        content.endText();
    }

    private void drawSectionText(PDPageContentStream content, String text, int x, int y, int fontSize, Color color) throws IOException {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, fontSize);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private void insertImage(PDDocument document, PDPageContentStream content, BufferedImage image, int x, int y, int width, int height) throws IOException {
        PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
        content.drawImage(pdImage, x, y, width, height);
    }

    private BufferedImage generateCategoryPieChart(List<TransactionResponse> transactions) {
        Map<String, BigDecimal> categoryTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                        TransactionResponse::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, TransactionResponse::getAmount, BigDecimal::add)
                ));

        DefaultPieDataset dataset = new DefaultPieDataset();
        categoryTotals.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(
                "Expenses by Category",
                dataset,
                true, true, false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelFont(new Font("SansSerif", Font.PLAIN, 10));
        plot.setSimpleLabels(true);
        plot.setLabelGap(0.02);

        return chart.createBufferedImage(500, 300);
    }

    private BufferedImage generateDateLineChart(List<TransactionResponse> transactions) {
        Map<LocalDate, BigDecimal> dailyTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> LocalDate.parse(t.getDate().substring(0, 10)),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, TransactionResponse::getAmount, BigDecimal::add)
                ));

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dailyTotals.forEach((date, amount) -> dataset.addValue(amount, "Expenses", date.toString()));

        JFreeChart chart = ChartFactory.createLineChart(
                "Daily Expenses",
                "Date",
                "Amount",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );

        return chart.createBufferedImage(500, 300);
    }

    private BufferedImage generateMiniPie(String title, BigDecimal total) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue(title, total);
        dataset.setValue("Remaining", 1); // background value to show 100%

        JFreeChart chart = ChartFactory.createPieChart(
                title + ": " + total + "₴",
                dataset,
                false, false, false
        );

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint(title, new Color(79, 129, 189));
        plot.setSectionPaint("Remaining", new Color(230, 230, 230));
        plot.setOutlineVisible(false);
        plot.setLabelGenerator(null);

        return chart.createBufferedImage(200, 150);
    }
}
