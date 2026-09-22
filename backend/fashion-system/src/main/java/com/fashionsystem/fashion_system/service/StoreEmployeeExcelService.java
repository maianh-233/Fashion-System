package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.employee.EmployeeResponse;
import com.fashionsystem.fashion_system.helper.excel.ExcelColumn;
import com.fashionsystem.fashion_system.helper.excel.ExcelHelper;
import com.fashionsystem.fashion_system.helper.excel.ExcelSheet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoreEmployeeExcelService {
    private final ExcelHelper excelHelper;

    public byte[] export(String storeCode, String storeName, List<EmployeeResponse> employees) {
        String sheetName = "Nhan vien";
        List<ExcelColumn<EmployeeResponse>> columns = List.of(
                new ExcelColumn<>("Mã nhân viên", EmployeeResponse::employeeCode),
                new ExcelColumn<>("Tên đăng nhập", EmployeeResponse::username),
                new ExcelColumn<>("Họ và tên", EmployeeResponse::fullName),
                new ExcelColumn<>("Email", EmployeeResponse::email),
                new ExcelColumn<>("Số điện thoại", EmployeeResponse::phone),
                new ExcelColumn<>("Chức danh", EmployeeResponse::jobTitle),
                new ExcelColumn<>("Loại hợp đồng", EmployeeResponse::employmentType),
                new ExcelColumn<>("Trạng thái", EmployeeResponse::employmentStatus),
                new ExcelColumn<>("Đang hoạt động", EmployeeResponse::active),
                new ExcelColumn<>("Vai trò", row -> row.roleCodes() == null ? "" : String.join(", ", row.roleCodes())),
                new ExcelColumn<>("Cửa hàng", row -> storeName == null ? storeCode : storeName));
        return excelHelper.export(new ExcelSheet<>(sheetName, columns, employees));
    }
}
