package com.fashionsystem.fashion_system.helper.pdf;

import java.util.List;

/** Generic PDF document specification with A4-friendly margin defaults. */
public record PdfDocumentData(
        String title,
        String header,
        String footer,
        List<PdfElement> elements,
        float marginTop,
        float marginRight,
        float marginBottom,
        float marginLeft) {
    public PdfDocumentData {
        elements = elements == null ? List.of() : List.copyOf(elements);
        if (marginTop <= 0) marginTop = 54;
        if (marginRight <= 0) marginRight = 54;
        if (marginBottom <= 0) marginBottom = 54;
        if (marginLeft <= 0) marginLeft = 54;
    }

    public PdfDocumentData(String title, List<PdfElement> elements) {
        this(title, null, null, elements, 54, 54, 54, 54);
    }
}
