package com.expensetracker.controller;

import com.expensetracker.dto.ReceiptScanResponse;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;

/**
 * Receipt OCR scanning and per-transaction receipt attachment/download.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * Scans an uploaded receipt image and returns a suggested transaction draft.
     * Returns ocrAvailable=false (with empty fields) when tesseract is unavailable —
     * the image is still stored and the client should fall back to manual entry.
     */
    @PostMapping("/receipts/scan")
    public ReceiptScanResponse scan(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestParam("image") MultipartFile image) {
        return receiptService.scan(image);
    }

    @PostMapping("/transactions/{id}/receipt")
    public Map<String, String> attach(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id,
                                      @RequestParam("image") MultipartFile image) {
        receiptService.attachReceipt(principal.getUserId(), id, image);
        return Map.of("message", "Receipt attached");
    }

    @GetMapping("/transactions/{id}/receipt")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id) {
        Path path = receiptService.receiptPath(principal.getUserId(), id);
        Resource resource = new FileSystemResource(path);
        String filename = path.getFileName().toString();
        String lower = filename.toLowerCase(java.util.Locale.ROOT);
        MediaType mediaType = lower.endsWith(".png") ? MediaType.IMAGE_PNG
                : lower.endsWith(".gif") ? MediaType.IMAGE_GIF
                : lower.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
