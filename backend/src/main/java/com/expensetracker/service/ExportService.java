package com.expensetracker.service;

import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.util.MoneyUtil;
import com.expensetracker.util.MonthUtil;
import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<Transaction> fetchMonth(Long userId, String month) {
        String key = (month == null || month.isBlank()) ? MonthUtil.currentMonthKey() : month;
        LocalDate[] range = MonthUtil.monthRange(key);
        return transactionRepository.search(userId, null, null, null, null, null,
                        range[0], range[1],
                        PageRequest.of(0, 100_000, Sort.by(Sort.Direction.DESC, "date")))
                .getContent();
    }

    public byte[] buildPdf(String month, List<Transaction> transactions) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph("Expense Tracker Report - " + month));
            doc.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            for (String h : List.of("Date", "Type", "Category", "Amount (INR)", "Note")) {
                table.addCell(h);
            }
            long income = 0, expense = 0;
            for (Transaction tx : transactions) {
                table.addCell(tx.getDate().format(DATE_FMT));
                table.addCell(tx.getType().jsonValue());
                table.addCell(tx.getCategory());
                table.addCell(MoneyUtil.toDecimalString(tx.getAmountPaise()));
                table.addCell(tx.getNote() == null ? "" : tx.getNote());
                if (tx.getType() == TransactionType.INCOME) {
                    income += tx.getAmountPaise();
                } else {
                    expense += tx.getAmountPaise();
                }
            }
            doc.add(table);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Total income: INR " + MoneyUtil.toDecimalString(income)));
            doc.add(new Paragraph("Total expense: INR " + MoneyUtil.toDecimalString(expense)));
            doc.add(new Paragraph("Net: INR " + MoneyUtil.toDecimalString(income - expense)));
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build PDF", e);
        }
    }

    public byte[] buildExcel(List<Transaction> transactions) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Transactions");
            String[] headers = {"Date", "Type", "Category", "Amount (INR)", "Tags", "Note"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (Transaction tx : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.getDate().format(DATE_FMT));
                row.createCell(1).setCellValue(tx.getType().jsonValue());
                row.createCell(2).setCellValue(tx.getCategory());
                row.createCell(3).setCellValue(MoneyUtil.toDecimalString(tx.getAmountPaise()));
                row.createCell(4).setCellValue(String.join(", ", tx.getTags()));
                row.createCell(5).setCellValue(tx.getNote() == null ? "" : tx.getNote());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build Excel file", e);
        }
    }
}
