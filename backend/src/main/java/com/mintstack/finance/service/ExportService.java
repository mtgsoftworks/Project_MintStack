package com.mintstack.finance.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mintstack.finance.dto.response.PortfolioItemResponse;
import com.mintstack.finance.dto.response.PortfolioResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for exporting portfolio data to Excel and PDF formats.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Export portfolio to Excel (XLSX) format.
     */
    public byte[] exportPortfolioToExcel(PortfolioResponse portfolio) {
        log.info("Exporting portfolio {} to Excel", portfolio.getId());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Portföy");

            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);
            CellStyle profitStyle = createProfitStyle(workbook, true);
            CellStyle lossStyle = createProfitStyle(workbook, false);

            int rowNum = 0;

            // Title
            Row titleRow = sheet.createRow(rowNum++);
            org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Portföy Raporu: " + portfolio.getName());
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Date
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.createCell(0).setCellValue("Tarih: " + LocalDateTime.now().format(DATE_FORMATTER));

            // Empty row
            rowNum++;

            // Summary info
            Row summaryRow1 = sheet.createRow(rowNum++);
            summaryRow1.createCell(0).setCellValue("Toplam Maliyet:");
            org.apache.poi.ss.usermodel.Cell costCell = summaryRow1.createCell(1);
            costCell.setCellValue(portfolio.getTotalCost() != null ? portfolio.getTotalCost().doubleValue() : 0);
            costCell.setCellStyle(currencyStyle);

            Row summaryRow2 = sheet.createRow(rowNum++);
            summaryRow2.createCell(0).setCellValue("Güncel Değer:");
            org.apache.poi.ss.usermodel.Cell valueCell = summaryRow2.createCell(1);
            valueCell.setCellValue(portfolio.getTotalValue() != null ? portfolio.getTotalValue().doubleValue() : 0);
            valueCell.setCellStyle(currencyStyle);

            Row summaryRow3 = sheet.createRow(rowNum++);
            summaryRow3.createCell(0).setCellValue("Kar/Zarar:");
            org.apache.poi.ss.usermodel.Cell plCell = summaryRow3.createCell(1);
            double profitLoss = portfolio.getProfitLoss() != null ? portfolio.getProfitLoss().doubleValue() : 0;
            plCell.setCellValue(profitLoss);
            plCell.setCellStyle(profitLoss >= 0 ? profitStyle : lossStyle);

            Row summaryRow4 = sheet.createRow(rowNum++);
            summaryRow4.createCell(0).setCellValue("Kar/Zarar (%):");
            org.apache.poi.ss.usermodel.Cell plPercentCell = summaryRow4.createCell(1);
            double profitLossPercent = portfolio.getProfitLossPercent() != null ? portfolio.getProfitLossPercent().doubleValue() : 0;
            plPercentCell.setCellValue(profitLossPercent / 100);
            plPercentCell.setCellStyle(percentStyle);

            // Empty row
            rowNum++;

            // Header row
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Sembol", "İsim", "Tür", "Adet", "Alış Fiyatı", "Güncel Fiyat", "Maliyet", "Değer", "K/Z", "K/Z %"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            if (portfolio.getItems() != null) {
                for (PortfolioItemResponse item : portfolio.getItems()) {
                    Row row = sheet.createRow(rowNum++);
                    
                    row.createCell(0).setCellValue(item.getInstrumentSymbol() != null ? item.getInstrumentSymbol() : "-");
                    row.createCell(1).setCellValue(item.getInstrumentName() != null ? item.getInstrumentName() : "-");
                    row.createCell(2).setCellValue(item.getInstrumentType() != null ? item.getInstrumentType().toString() : "-");
                    
                    org.apache.poi.ss.usermodel.Cell quantityCell = row.createCell(3);
                    quantityCell.setCellValue(item.getQuantity() != null ? item.getQuantity().doubleValue() : 0);

                    org.apache.poi.ss.usermodel.Cell purchasePriceCell = row.createCell(4);
                    purchasePriceCell.setCellValue(item.getPurchasePrice() != null ? item.getPurchasePrice().doubleValue() : 0);
                    purchasePriceCell.setCellStyle(currencyStyle);

                    org.apache.poi.ss.usermodel.Cell currentPriceCell = row.createCell(5);
                    currentPriceCell.setCellValue(item.getCurrentPrice() != null ? item.getCurrentPrice().doubleValue() : 0);
                    currentPriceCell.setCellStyle(currencyStyle);

                    org.apache.poi.ss.usermodel.Cell totalCostCell = row.createCell(6);
                    totalCostCell.setCellValue(item.getTotalCost() != null ? item.getTotalCost().doubleValue() : 0);
                    totalCostCell.setCellStyle(currencyStyle);

                    org.apache.poi.ss.usermodel.Cell currentValueCell = row.createCell(7);
                    currentValueCell.setCellValue(item.getCurrentValue() != null ? item.getCurrentValue().doubleValue() : 0);
                    currentValueCell.setCellStyle(currencyStyle);

                    org.apache.poi.ss.usermodel.Cell itemPlCell = row.createCell(8);
                    double itemPl = item.getProfitLoss() != null ? item.getProfitLoss().doubleValue() : 0;
                    itemPlCell.setCellValue(itemPl);
                    itemPlCell.setCellStyle(itemPl >= 0 ? profitStyle : lossStyle);

                    org.apache.poi.ss.usermodel.Cell itemPlPercentCell = row.createCell(9);
                    double itemPlPercent = item.getProfitLossPercent() != null ? item.getProfitLossPercent().doubleValue() : 0;
                    itemPlPercentCell.setCellValue(itemPlPercent / 100);
                    itemPlPercentCell.setCellStyle(percentStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting portfolio to Excel", e);
            throw new RuntimeException("Excel dosyası oluşturulamadı", e);
        }
    }

    /**
     * Export portfolio to PDF format.
     */
    public byte[] exportPortfolioToPdf(PortfolioResponse portfolio) {
        log.info("Exporting portfolio {} to PDF", portfolio.getId());

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Title
            Paragraph title = new Paragraph("Portföy Raporu")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(title);

            Paragraph portfolioName = new Paragraph(portfolio.getName())
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10);
            document.add(portfolioName);

            Paragraph date = new Paragraph("Tarih: " + LocalDateTime.now().format(DATE_FORMATTER))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(date);

            // Summary table
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            addSummaryRow(summaryTable, "Toplam Maliyet", formatCurrency(portfolio.getTotalCost()));
            addSummaryRow(summaryTable, "Güncel Değer", formatCurrency(portfolio.getTotalValue()));
            
            String plText = formatCurrency(portfolio.getProfitLoss());
            DeviceRgb plColor = portfolio.getProfitLoss() != null && portfolio.getProfitLoss().compareTo(BigDecimal.ZERO) >= 0
                    ? new DeviceRgb(0, 128, 0) : new DeviceRgb(220, 20, 60);
            addSummaryRowColored(summaryTable, "Kar/Zarar", plText, plColor);
            addSummaryRow(summaryTable, "Kar/Zarar (%)", formatPercent(portfolio.getProfitLossPercent()));
            addSummaryRow(summaryTable, "Varlık Sayısı", String.valueOf(portfolio.getItemCount()));

            document.add(summaryTable);

            // Items table header
            document.add(new Paragraph("Portföy Varlıkları")
                    .setFontSize(14)
                    .setBold()
                    .setMarginBottom(10));

            // Items table
            if (portfolio.getItems() != null && !portfolio.getItems().isEmpty()) {
                Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{12, 20, 10, 12, 12, 12, 12, 10}))
                        .useAllAvailableWidth();

                // Headers
                String[] headers = {"Sembol", "İsim", "Adet", "Alış", "Güncel", "Maliyet", "Değer", "K/Z %"};
                for (String header : headers) {
                    itemsTable.addHeaderCell(new Cell()
                            .add(new Paragraph(header).setBold().setFontSize(9))
                            .setBackgroundColor(new DeviceRgb(240, 240, 240))
                            .setTextAlignment(TextAlignment.CENTER));
                }

                // Data rows
                for (PortfolioItemResponse item : portfolio.getItems()) {
                    itemsTable.addCell(createCell(item.getInstrumentSymbol() != null ? item.getInstrumentSymbol() : "-", 9));
                    itemsTable.addCell(createCell(truncate(item.getInstrumentName(), 20), 8));
                    itemsTable.addCell(createCell(formatNumber(item.getQuantity()), 9));
                    itemsTable.addCell(createCell(formatCurrency(item.getPurchasePrice()), 9));
                    itemsTable.addCell(createCell(formatCurrency(item.getCurrentPrice()), 9));
                    itemsTable.addCell(createCell(formatCurrency(item.getTotalCost()), 9));
                    itemsTable.addCell(createCell(formatCurrency(item.getCurrentValue()), 9));

                    // Colored P/L cell
                    Cell plCell = new Cell()
                            .add(new Paragraph(formatPercent(item.getProfitLossPercent())).setFontSize(9))
                            .setTextAlignment(TextAlignment.RIGHT);
                    if (item.getProfitLossPercent() != null && item.getProfitLossPercent().compareTo(BigDecimal.ZERO) >= 0) {
                        plCell.setFontColor(new DeviceRgb(0, 128, 0));
                    } else {
                        plCell.setFontColor(new DeviceRgb(220, 20, 60));
                    }
                    itemsTable.addCell(plCell);
                }

                document.add(itemsTable);
            } else {
                document.add(new Paragraph("Bu portföyde henüz varlık bulunmamaktadır.")
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY));
            }

            // Footer
            document.add(new Paragraph("\n\nBu rapor MintStack Finance Portal tarafından oluşturulmuştur.")
                    .setFontSize(8)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting portfolio to PDF", e);
            throw new RuntimeException("PDF dosyası oluşturulamadı", e);
        }
    }

    // Helper methods for Excel
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00 ₺"));
        return style;
    }

    private CellStyle createPercentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("0.00%"));
        return style;
    }

    private CellStyle createProfitStyle(Workbook workbook, boolean isProfit) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00 ₺"));
        Font font = workbook.createFont();
        font.setColor(isProfit ? IndexedColors.GREEN.getIndex() : IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }

    // Helper methods for PDF
    private void addSummaryRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(10)));
        table.addCell(new Cell().add(new Paragraph(value).setFontSize(10)).setTextAlignment(TextAlignment.RIGHT));
    }

    private void addSummaryRowColored(Table table, String label, String value, DeviceRgb color) {
        table.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(10)));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFontSize(10).setFontColor(color))
                .setTextAlignment(TextAlignment.RIGHT));
    }

    private Cell createCell(String text, int fontSize) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "-").setFontSize(fontSize))
                .setTextAlignment(TextAlignment.CENTER);
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) return "0,00 ₺";
        return String.format("%,.2f ₺", value);
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return "%0,00";
        return String.format("%%%,.2f", value);
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) return "0";
        return String.format("%,.2f", value);
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "-";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
