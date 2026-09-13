package com.fashionsystem.fashion_system.helper.pdf;

import java.util.Arrays;

/** Encoded PNG/JPEG image block. */
public record PdfImage(byte[] content, float width, float height, PdfAlignment alignment, float spacingAfter)
        implements PdfElement {
    public PdfImage {
        content = content == null ? null : Arrays.copyOf(content, content.length);
    }
    @Override public byte[] content() { return content == null ? null : Arrays.copyOf(content, content.length); }
}
