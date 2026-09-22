package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fashionsystem.fashion_system.dto.employee.EmployeeResponse;
import com.fashionsystem.fashion_system.helper.excel.ExcelHelper;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

class StoreEmployeeExcelServiceTest {
    @Test
    void exportsAllEmployeeColumnsToOneSheet() throws Exception {
        var employee = new EmployeeResponse(UUID.randomUUID(), "anna", "NV001", "Anna Nguyen",
                "anna@example.com", "0900000000", "Sales", "FULL_TIME", "ACTIVE",
                null, true, false, null, null, List.of("STAFF"), UUID.randomUUID(), "CH001",
                "Flagship", null, null, null, null, null, null, null, null);

        byte[] bytes = new StoreEmployeeExcelService(new ExcelHelper()).export("CH001", "Flagship", List.of(employee));

        try (var workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheet("Nhan vien")).isNotNull();
            assertThat(workbook.getSheet("Nhan vien").getLastRowNum()).isEqualTo(1);
            assertThat(workbook.getSheet("Nhan vien").getRow(1).getCell(2).getStringCellValue()).isEqualTo("Anna Nguyen");
        }
    }
}
