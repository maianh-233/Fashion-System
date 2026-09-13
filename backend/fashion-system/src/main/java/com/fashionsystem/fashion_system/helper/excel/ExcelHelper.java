package com.fashionsystem.fashion_system.helper.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Apache POI exporter for typed, entity-independent workbook specifications. */
@Component
public class ExcelHelper {

    public byte[] export(ExcelSheet<?> sheet) {
        return export(new ExcelWorkbookData(List.of(sheet)));
    }

    public byte[] export(ExcelWorkbookData workbookData) {
        if (workbookData == null || workbookData.sheets().isEmpty()) {
            throw new IllegalArgumentException("At least one Excel sheet is required");
        }
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (ExcelSheet<?> sheet : workbookData.sheets()) writeSheet(workbook, sheet);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof IllegalArgumentException illegalArgument) throw illegalArgument;
            throw new ExcelExportException("Excel export failed", exception);
        }
    }

    private <T> void writeSheet(Workbook workbook, ExcelSheet<T> specification) {
        validateSheet(specification);
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet(specification.name().trim());
        StyleRegistry styles = new StyleRegistry(workbook);
        Row header = sheet.createRow(0);
        for (int columnIndex = 0; columnIndex < specification.columns().size(); columnIndex++) {
            Cell cell = header.createCell(columnIndex);
            cell.setCellValue(specification.columns().get(columnIndex).header());
            cell.setCellStyle(styles.header());
        }
        int rowIndex = 1;
        for (T value : specification.rows()) {
            Row row = sheet.createRow(rowIndex++);
            for (int columnIndex = 0; columnIndex < specification.columns().size(); columnIndex++) {
                ExcelColumn<T> column = specification.columns().get(columnIndex);
                Cell cell = row.createCell(columnIndex);
                Object cellValue = column.valueExtractor().apply(value);
                writeCell(cell, cellValue);
                cell.setCellStyle(styles.data(column));
            }
        }
        if (specification.freezeHeader()) sheet.createFreezePane(0, 1);
        if (specification.autoSizeColumns()) {
            for (int index = 0; index < specification.columns().size(); index++) {
                sheet.autoSizeColumn(index);
                sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 512, 80 * 256));
            }
        }
    }

    private void validateSheet(ExcelSheet<?> sheet) {
        if (sheet == null) throw new IllegalArgumentException("Excel sheet is required");
        if (!StringUtils.hasText(sheet.name())) throw new IllegalArgumentException("Excel sheet name is required");
        if (sheet.name().length() > 31 || sheet.name().matches(".*[\\\\/?*\\[\\]:].*")) {
            throw new IllegalArgumentException("Invalid Excel sheet name");
        }
        if (sheet.columns().isEmpty()) throw new IllegalArgumentException("At least one Excel column is required");
        for (ExcelColumn<?> column : sheet.columns()) {
            if (column == null || !StringUtils.hasText(column.header()) || column.valueExtractor() == null) {
                throw new IllegalArgumentException("Every Excel column requires a header and value extractor");
            }
        }
    }

    private void writeCell(Cell cell, Object value) {
        if (value == null) { cell.setBlank(); return; }
        if (value instanceof LocalDate date) { cell.setCellValue(date); return; }
        if (value instanceof LocalDateTime dateTime) { cell.setCellValue(dateTime); return; }
        if (value instanceof BigDecimal decimal) { cell.setCellValue(decimal.doubleValue()); return; }
        if (value instanceof Number number) { cell.setCellValue(number.doubleValue()); return; }
        if (value instanceof Boolean bool) { cell.setCellValue(bool); return; }
        cell.setCellValue(value.toString());
    }

    private static final class StyleRegistry {
        private final Workbook workbook;
        private final CellStyle header;
        private final Map<ExcelColumn<?>, CellStyle> dataStyles = new IdentityHashMap<>();
        private final Map<ExcelCellFormat, String> defaults = new EnumMap<>(ExcelCellFormat.class);

        private StyleRegistry(Workbook workbook) {
            this.workbook = workbook;
            defaults.put(ExcelCellFormat.TEXT, "General");
            defaults.put(ExcelCellFormat.NUMBER, "#,##0.00");
            defaults.put(ExcelCellFormat.CURRENCY, "#,##0.00");
            defaults.put(ExcelCellFormat.DATE, "dd/mm/yyyy");
            defaults.put(ExcelCellFormat.DATE_TIME, "dd/mm/yyyy hh:mm");
            Font font = workbook.createFont();
            font.setBold(true);
            header = workbook.createCellStyle();
            header.setFont(font);
            header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);
            applyBorders(header);
        }

        private CellStyle header() { return header; }

        private CellStyle data(ExcelColumn<?> column) {
            return dataStyles.computeIfAbsent(column, ignored -> {
                CellStyle style = workbook.createCellStyle();
                applyBorders(style);
                style.setAlignment(toPoi(column.alignment()));
                DataFormat dataFormat = workbook.createDataFormat();
                ExcelCellFormat format = column.format() == null ? ExcelCellFormat.TEXT : column.format();
                String pattern = StringUtils.hasText(column.formatPattern())
                        ? column.formatPattern() : defaults.get(format);
                style.setDataFormat(dataFormat.getFormat(pattern));
                return style;
            });
        }

        private static void applyBorders(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        }

        private HorizontalAlignment toPoi(ExcelAlignment alignment) {
            if (alignment == null) return HorizontalAlignment.LEFT;
            return switch (alignment) {
                case LEFT -> HorizontalAlignment.LEFT;
                case CENTER -> HorizontalAlignment.CENTER;
                case RIGHT -> HorizontalAlignment.RIGHT;
            };
        }
    }
}
