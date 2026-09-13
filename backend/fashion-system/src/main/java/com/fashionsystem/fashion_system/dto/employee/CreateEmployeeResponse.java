package com.fashionsystem.fashion_system.dto.employee;

/** Kết quả tạo mới; mật khẩu tạm chỉ xuất hiện trong response này đúng một lần. */
public record CreateEmployeeResponse(
        EmployeeResponse employee,
        String temporaryPassword) {
}
