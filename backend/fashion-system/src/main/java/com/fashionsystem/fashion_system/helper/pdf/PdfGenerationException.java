package com.fashionsystem.fashion_system.helper.pdf;

/** Stable exception boundary for PDF rendering failures. */
public class PdfGenerationException extends RuntimeException {
    public PdfGenerationException(String message, Throwable cause) { super(message, cause); }
}
