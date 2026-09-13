package com.fashionsystem.fashion_system.helper.pdf;

import java.util.List;

/** Simple table whose rows remain independent from entities and business DTOs. */
public record PdfTable(List<String> headers, List<List<String>> rows, List<Float> columnWidths, float fontSize)
        implements PdfElement {
    public PdfTable {
        headers = headers == null ? List.of() : List.copyOf(headers);
        rows = rows == null ? List.of() : rows.stream().map(List::copyOf).toList();
        columnWidths = columnWidths == null ? List.of() : List.copyOf(columnWidths);
    }

    public PdfTable(List<String> headers, List<List<String>> rows) {
        this(headers, rows, List.of(), 9);
    }
}
