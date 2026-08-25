package com.fashionsystem.fashion_system.entity;

/** Phạm vi dữ liệu mà một permission được phép tác động. */
public enum PermissionScope {
    SELF,
    TEAM,
    DEPARTMENT,
    ALL;

    /** Trả về true khi scope hiện tại bao phủ scope được yêu cầu. */
    public boolean covers(PermissionScope requiredScope) {
        return ordinal() >= requiredScope.ordinal();
    }
}
