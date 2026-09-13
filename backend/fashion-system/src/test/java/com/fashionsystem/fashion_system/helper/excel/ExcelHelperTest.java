package com.fashionsystem.fashion_system.helper.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class ExcelHelperTest {
    private final ExcelHelper helper = new ExcelHelper();

    @Test
    void exportsHeadersWithEmptyData() throws Exception {
        ExcelSheet<ExportRow> specification = sheet("Empty", List.of());
        byte[] bytes = helper.export(specification);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Empty");
            assertThat(sheet.getLastRowNum()).isZero();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Name");
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
        }
    }

    @Test
    void exportsNormalDataDateCurrencyAndStyles() throws Exception {
        byte[] bytes = helper.export(sheet("Report", List.of(
                new ExportRow("Áo khoác", LocalDate.of(2026, 9, 1), new BigDecimal("125000.50")))));
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Report");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Áo khoác");
            assertThat(sheet.getRow(1).getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(sheet.getRow(1).getCell(1).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy");
            assertThat(sheet.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(125000.50);
            assertThat(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex())
                    .isNotEqualTo(sheet.getRow(1).getCell(0).getCellStyle().getFontIndex());
        }
    }

    @Test
    void exportsMultipleSheets() throws Exception {
        ExcelWorkbookData data = new ExcelWorkbookData(List.of(
                sheet("Products", List.of(new ExportRow("A", LocalDate.now(), BigDecimal.ONE))),
                sheet("Orders", List.of(new ExportRow("B", LocalDate.now(), BigDecimal.TEN)))));
        byte[] bytes = helper.export(data);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("Products");
            assertThat(workbook.getSheetName(1)).isEqualTo("Orders");
        }
    }

    private ExcelSheet<ExportRow> sheet(String name, List<ExportRow> rows) {
        return new ExcelSheet<>(name, List.of(
                new ExcelColumn<>("Name", ExportRow::name),
                new ExcelColumn<>("Date", ExportRow::date, ExcelCellFormat.DATE, null, ExcelAlignment.CENTER),
                new ExcelColumn<>("Amount", ExportRow::amount, ExcelCellFormat.CURRENCY, "#,##0.00", ExcelAlignment.RIGHT)),
                rows);
    }

    private record ExportRow(String name, LocalDate date, BigDecimal amount) { }
}
