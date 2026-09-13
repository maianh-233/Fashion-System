package com.fashionsystem.fashion_system.helper.pdf;

/** Paragraph content with basic typography. */
public record PdfText(String text, float fontSize, boolean bold, PdfAlignment alignment, float spacingAfter)
        implements PdfElement {
    public PdfText(String text) { this(text, 11, false, PdfAlignment.LEFT, 8); }
}
