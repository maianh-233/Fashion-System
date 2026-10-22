package com.fashionsystem.fashion_system.entity;

/** Bên thực hiện một thay đổi trên đơn hàng. */
public enum OrderActorType {
    /** Nhân viên (bảng users). */
    USER,
    /** Khách hàng (bảng customers). */
    CUSTOMER,
    /** Tác vụ tự động của hệ thống. */
    SYSTEM,
    /** Bên vận chuyển, thông qua webhook. */
    SHIPPER
}
