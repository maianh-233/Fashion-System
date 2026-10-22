package com.fashionsystem.fashion_system.dto.storefront;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Cửa hàng hiển thị cho khách: trang hệ thống cửa hàng và tra cửa hàng còn hàng. */
public final class StorefrontStoreDtos {
    private StorefrontStoreDtos() {
    }

    public record Store(UUID id, String code, String name, String address, String phone, BigDecimal latitude,
                        BigDecimal longitude, String openingHours, String imageUrl, List<String> services) {
    }

    /** Tình trạng hàng tại quầy: IN_STOCK hoặc LOW_STOCK; không trả số lượng chính xác. */
    public record Availability(Store store, String stock) {
    }
}
