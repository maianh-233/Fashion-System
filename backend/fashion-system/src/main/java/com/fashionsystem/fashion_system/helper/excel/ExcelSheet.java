package com.fashionsystem.fashion_system.helper.excel;

import java.util.List;

/** Generic sheet specification for headers, columns and rows. */
public record ExcelSheet<T>(
        String name,
        List<ExcelColumn<T>> columns,
        List<T> rows,
        boolean freezeHeader,
        boolean autoSizeColumns) {
    public ExcelSheet {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public ExcelSheet(String name, List<ExcelColumn<T>> columns, List<T> rows) {
        this(name, columns, rows, true, true);
    }
}
