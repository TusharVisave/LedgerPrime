package com.expensetracker.service;

import com.expensetracker.dto.ReceiptScanResponse;
import com.expensetracker.entity.Transaction;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Receipt scanning via Tess4J OCR. Degrades gracefully: when the tesseract
 * native library (or tessdata) is absent at runtime, the image is still stored
 * and the response falls back to manual entry with ocrAvailable=false.
 */
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptService.class);

    private static final Pattern NUMBER = Pattern.compile("([\\d,]+(?:\\.\\d{1,2})?)");
    private static final Pattern TOTAL_LINE = Pattern.compile(
            "(?i)(?:grand\\s*total|total|amount\\s*(?:due|payable)?|net\\s*amount)\\s*[:\\-]?\\s*(?:₹|rs\\.?)?\\s*([\\d,]+(?:\\.\\d{1,2})?)");
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{1,2}[/\\-.\\s]\\d{1,2}[/\\-.\\s]\\d{2,4})|(\\d{4}-\\d{2}-\\d{2})");
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
    };

    private final TransactionRepository transactionRepository;
    private final CategoryKeywordService categoryKeywords;

    @Value("${app.receipts.dir:${java.io.tmpdir}/expense-receipts}")
    private String receiptsDir;

    /** Scans an uploaded receipt image and returns a suggested transaction draft. */
    public ReceiptScanResponse scan(MultipartFile image) {
        validateImage(image);
        String storedName = store(image);

        String ocrText = null;
        try {
            ocrText = runOcr(image);
        } catch (Throwable t) {
            // Native tesseract library or tessdata missing — degrade gracefully.
            log.warn("OCR unavailable (tesseract native library missing or failed): {}", t.toString());
        }

        boolean ocrAvailable = ocrText != null && !ocrText.isBlank();
        String merchant = null;
        String amount = null;
        String date = null;
        if (ocrAvailable) {
            merchant = extractMerchant(ocrText);
            amount = extractAmount(ocrText);
            date = extractDate(ocrText);
        }
        String category = categoryKeywords.suggest(ocrText != null ? ocrText : "");

        return ReceiptScanResponse.builder()
                .merchant(merchant)
                .amount(amount)
                .date(date)
                .category(category)
                .rawText(ocrAvailable ? ocrText : null)
                .ocrAvailable(ocrAvailable)
                .storedImage(storedName)
                .build();
    }

    /** Attaches a receipt image to a transaction owned by the user. */
    @Transactional
    public void attachReceipt(Long userId, Long transactionId, MultipartFile image) {
        validateImage(image);
        Transaction transaction = transactionRepository.findById(transactionId)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        String storedName = store(image);
        transaction.setReceiptImagePath(storedName);
        transactionRepository.save(transaction);
    }

    /** Resolves the stored receipt file for a transaction owned by the user. */
    @Transactional(readOnly = true)
    public Path receiptPath(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .filter(t -> t.getUser().getId().equals(userId))
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));
        if (transaction.getReceiptImagePath() == null) {
            throw ApiException.notFound("No receipt attached to this transaction");
        }
        Path path = receiptDir().resolve(transaction.getReceiptImagePath()).normalize();
        if (!path.startsWith(receiptDir()) || !Files.isRegularFile(path)) {
            throw ApiException.notFound("Receipt file not found");
        }
        return path;
    }

    /**
     * Runs Tess4J OCR. Throws (including UnsatisfiedLinkError) when the
     * tesseract native library is absent — the caller degrades gracefully.
     */
    private String runOcr(MultipartFile image) throws Exception {
        net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
        try (InputStream in = image.getInputStream()) {
            BufferedImage buffered = ImageIO.read(in);
            if (buffered == null) {
                throw new IOException("Could not decode image");
            }
            return tesseract.doOCR(buffered);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw ApiException.badRequest("An image file is required");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw ApiException.badRequest("Only image files are accepted");
        }
        if (image.getSize() > 10 * 1024 * 1024) {
            throw ApiException.badRequest("Image must be at most 10 MB");
        }
    }

    private String store(MultipartFile image) {
        try {
            Path dir = receiptDir();
            Files.createDirectories(dir);
            String ext = extension(image.getOriginalFilename());
            String name = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
            try (InputStream in = image.getInputStream()) {
                Files.copy(in, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            }
            return name;
        } catch (IOException e) {
            throw ApiException.badRequest("Could not store receipt image: " + e.getMessage());
        }
    }

    private Path receiptDir() {
        return Path.of(receiptsDir).toAbsolutePath().normalize();
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String extractMerchant(String text) {
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.length() >= 3 && trimmed.matches(".*[a-zA-Z]{3,}.*")) {
                return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
            }
        }
        return null;
    }

    private String extractAmount(String text) {
        Matcher total = TOTAL_LINE.matcher(text);
        BigDecimal best = null;
        if (total.find()) {
            best = parseNumber(total.group(1));
        }
        if (best == null) {
            Matcher numbers = NUMBER.matcher(text);
            while (numbers.find()) {
                BigDecimal value = parseNumber(numbers.group(1));
                if (value != null && (best == null || value.compareTo(best) > 0)) {
                    best = value;
                }
            }
        }
        if (best == null || best.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return MoneyUtil.toDecimalString(MoneyUtil.toPaise(best.toPlainString()));
    }

    private BigDecimal parseNumber(String raw) {
        try {
            return new BigDecimal(raw.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractDate(String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        while (matcher.find()) {
            String raw = matcher.group(0).trim();
            for (DateTimeFormatter format : DATE_FORMATS) {
                try {
                    LocalDate date = LocalDate.parse(raw, format);
                    if (!date.isAfter(LocalDate.now())) {
                        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    }
                } catch (Exception ignored) {
                    // try the next format
                }
            }
        }
        return null;
    }
}
