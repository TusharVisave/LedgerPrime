package com.expensetracker.dto;

import lombok.Builder;
import lombok.Value;

/** Suggested transaction pre-filled from a receipt scan (OCR may be unavailable). */
@Value
@Builder
public class ReceiptScanResponse {
    String merchant;
    /** Amount as decimal string, e.g. "250.75". Null when not detected. */
    String amount;
    /** Date as yyyy-MM-dd. Null when not detected. */
    String date;
    /** Suggested category name. Never null. */
    String category;
    /** Raw OCR text (may be null when OCR unavailable). */
    String rawText;
    /** False when the tesseract native library is absent — image is still stored. */
    boolean ocrAvailable;
    /** Stored image filename, usable for attachment. */
    String storedImage;
}
