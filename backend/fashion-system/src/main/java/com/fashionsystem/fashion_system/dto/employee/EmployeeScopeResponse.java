package com.fashionsystem.fashion_system.dto.employee;

import java.util.UUID;

public record EmployeeScopeResponse(String scope, UUID storeId, String storeCode, String storeName) {
}
