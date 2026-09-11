package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Store;
import java.util.UUID;

public record EmployeeDataScope(
        Kind kind,
        UUID storeId,
        String storeCode,
        String storeName,
        String permissionCode) {

    public enum Kind {
        ALL,
        STORE
    }

    public static EmployeeDataScope all(String permissionCode) {
        return new EmployeeDataScope(Kind.ALL, null, null, null, permissionCode);
    }

    public static EmployeeDataScope store(Store store, String permissionCode) {
        return new EmployeeDataScope(
                Kind.STORE, store.getId(), store.getCode(), store.getName(), permissionCode);
    }

    public boolean isGlobal() {
        return kind == Kind.ALL;
    }
}
