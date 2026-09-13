package com.fashionsystem.fashion_system.dto.employee;

/** Số liệu tổng quan theo đúng phạm vi cửa hàng của người đang đăng nhập. */
public record EmployeeSummaryResponse(long total, long active, long locked, long newThisMonth) {
}
