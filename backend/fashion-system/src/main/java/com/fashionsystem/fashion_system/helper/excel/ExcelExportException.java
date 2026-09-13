package com.fashionsystem.fashion_system.helper.excel;

/** Stable exception boundary for workbook generation failures. */
public class ExcelExportException extends RuntimeException {
    public ExcelExportException(String message, Throwable cause) { super(message, cause); }
}
