package com.fashionsystem.fashion_system.shipping;

import com.fashionsystem.fashion_system.entity.ShipmentStatus;
import java.util.Map;
import java.util.Optional;

/**
 * Ánh xạ trạng thái đơn Lalamove (ORDER_STATUS_CHANGED) sang trạng thái chuyến nội bộ.
 *
 * <pre>
 * ASSIGNING_DRIVER → ASSIGNING_DRIVER  đang tìm tài xế
 * ON_GOING         → DRIVER_ASSIGNED   tài xế đã nhận, đang tới lấy hàng
 * PICKED_UP        → PICKED_UP         đã lấy hàng, đang giao   (đơn: SHIPPING)
 * COMPLETED        → COMPLETED         giao thành công          (đơn: COMPLETED)
 * EXPIRED          → FAILED            không tìm được tài xế
 * REJECTED         → FAILED            tài xế từ chối
 * CANCELED         → FAILED            chuyến bị hủy (trừ khi chính cửa hàng hủy)
 * </pre>
 * Trạng thái lạ trả về empty: ghi log và bỏ qua, không làm hỏng dữ liệu.
 */
public final class LalamoveStatusMapping {
    private static final Map<String, ShipmentStatus> STATUSES = Map.of(
            "ASSIGNING_DRIVER", ShipmentStatus.ASSIGNING_DRIVER,
            "ON_GOING", ShipmentStatus.DRIVER_ASSIGNED,
            "PICKED_UP", ShipmentStatus.PICKED_UP,
            "COMPLETED", ShipmentStatus.COMPLETED,
            "EXPIRED", ShipmentStatus.FAILED,
            "REJECTED", ShipmentStatus.FAILED,
            "CANCELED", ShipmentStatus.FAILED);

    private static final Map<String, String> FAILURE_LABELS = Map.of(
            "EXPIRED", "Lalamove không tìm được tài xế",
            "REJECTED", "Tài xế từ chối chuyến",
            "CANCELED", "Chuyến bị hủy bên Lalamove");

    private LalamoveStatusMapping() {
    }

    public static Optional<ShipmentStatus> toInternal(String providerStatus) {
        return Optional.ofNullable(providerStatus == null ? null : STATUSES.get(providerStatus));
    }

    public static String failureLabel(String providerStatus, String cancelReason) {
        String label = FAILURE_LABELS.getOrDefault(providerStatus, "Chuyến không thành công");
        return cancelReason == null || cancelReason.isBlank() ? label : label + " (" + cancelReason + ")";
    }
}
