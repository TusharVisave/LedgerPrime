package com.expensetracker.controller;

import com.expensetracker.entity.Transaction;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.ExportService;
import com.expensetracker.util.MonthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/transactions.pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestParam(required = false) String month) {
        String key = (month == null || month.isBlank()) ? MonthUtil.currentMonthKey() : month;
        List<Transaction> transactions = exportService.fetchMonth(principal.getUserId(), key);
        byte[] pdf = exportService.buildPdf(key, transactions);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("expense-report-" + key + ".pdf", StandardCharsets.UTF_8).toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/transactions.xlsx")
    public ResponseEntity<byte[]> excel(@AuthenticationPrincipal UserPrincipal principal,
                                        @RequestParam(required = false) String month) {
        String key = (month == null || month.isBlank()) ? MonthUtil.currentMonthKey() : month;
        List<Transaction> transactions = exportService.fetchMonth(principal.getUserId(), key);
        byte[] xlsx = exportService.buildExcel(transactions);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("expense-report-" + key + ".xlsx", StandardCharsets.UTF_8).toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}
