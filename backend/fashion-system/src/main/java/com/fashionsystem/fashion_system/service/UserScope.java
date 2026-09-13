package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.Store;
import java.util.UUID;

/** Phạm vi định danh của nhân viên, độc lập với quyền RBAC hiệu lực. */
public record UserScope(Kind kind, UUID storeId, String storeCode, String storeName) {
    public enum Kind {
        GLOBAL,
        STORE
    }

    /** Tạo phạm vi nhân viên cấp chuỗi. */
    public static UserScope global() {
        return new UserScope(Kind.GLOBAL, null, null, null);
    }

    /** Tạo phạm vi nhân viên thuộc đúng một cửa hàng hoạt động. */
    public static UserScope store(Store store) {
        return new UserScope(Kind.STORE, store.getId(), store.getCode(), store.getName());
    }

    /** Trả về true khi nhân viên không bị giới hạn vào một Store. */
    public boolean isGlobal() {
        return kind == Kind.GLOBAL;
    }
}
