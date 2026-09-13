package com.fashionsystem.fashion_system.helper.excel;

import java.util.function.Function;

/** Typed column mapping that keeps workbook infrastructure independent of entities. */
public record ExcelColumn<T>(
        String header,
        Function<T, ?> valueExtractor,
        ExcelCellFormat format,
        String formatPattern,
        ExcelAlignment alignment) {
    public ExcelColumn(String header, Function<T, ?> valueExtractor) {
        this(header, valueExtractor, ExcelCellFormat.TEXT, null, ExcelAlignment.LEFT);
    }
}
