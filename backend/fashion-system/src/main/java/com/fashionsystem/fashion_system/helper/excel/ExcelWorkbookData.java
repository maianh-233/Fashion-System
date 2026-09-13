package com.fashionsystem.fashion_system.helper.excel;

import java.util.List;

/** Multi-sheet workbook specification. */
public record ExcelWorkbookData(List<ExcelSheet<?>> sheets) {
    public ExcelWorkbookData {
        sheets = sheets == null ? List.of() : List.copyOf(sheets);
    }
}
