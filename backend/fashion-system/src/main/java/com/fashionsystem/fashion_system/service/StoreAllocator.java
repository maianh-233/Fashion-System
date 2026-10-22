package com.fashionsystem.fashion_system.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Phân đơn online về cửa hàng theo khoảng cách (Haversine) và tồn kho online.
 *
 * <ol>
 *   <li>Cửa hàng gần nhất đủ hàng cho toàn bộ giỏ nhận cả đơn.</li>
 *   <li>Không cửa hàng nào đủ: lấy tối đa ở cửa hàng gần nhất, phần còn thiếu chuyển sang cửa hàng kế
 *       tiếp theo khoảng cách cho tới khi đủ (tách đơn).</li>
 *   <li>Tổng tồn của mọi cửa hàng vẫn không đủ: báo hết hàng, không tạo đơn.</li>
 * </ol>
 */
@Component
public class StoreAllocator {
    private static final double EARTH_RADIUS_KM = 6371.0088;

    public record StoreLocation(UUID storeId, String code, double latitude, double longitude) {
    }

    /** Phần đơn giao cho một cửa hàng: biến thể → số lượng. */
    public record Part(UUID storeId, double distanceKm, Map<UUID, Integer> quantities) {
    }

    /** @param shortages biến thể thiếu hàng → số lượng còn thiếu sau khi gom mọi cửa hàng */
    public record Result(List<Part> parts, Map<UUID, Integer> shortages) {
        public boolean fulfilled() {
            return shortages.isEmpty();
        }

        public boolean split() {
            return parts.size() > 1;
        }
    }

    /**
     * @param demand biến thể → số lượng khách đặt
     * @param stock  cửa hàng → (biến thể → tồn online)
     */
    public Result allocate(double latitude, double longitude, Map<UUID, Integer> demand, List<StoreLocation> stores,
                           Map<UUID, Map<UUID, Integer>> stock) {
        List<StoreLocation> byDistance = stores.stream()
                .sorted(Comparator.comparingDouble((StoreLocation s) -> distanceKm(latitude, longitude, s.latitude(), s.longitude()))
                        .thenComparing(StoreLocation::code, Comparator.nullsLast(String::compareTo)))
                .toList();

        Map<UUID, Integer> shortages = new LinkedHashMap<>();
        demand.forEach((variant, quantity) -> {
            int available = byDistance.stream().mapToInt(s -> available(stock, s.storeId(), variant)).sum();
            if (available < quantity) shortages.put(variant, quantity - available);
        });
        if (!shortages.isEmpty()) return new Result(List.of(), shortages);

        for (StoreLocation store : byDistance) {
            boolean coversAll = demand.entrySet().stream()
                    .allMatch(e -> available(stock, store.storeId(), e.getKey()) >= e.getValue());
            if (coversAll) return new Result(List.of(part(store, latitude, longitude, new LinkedHashMap<>(demand))), Map.of());
        }

        Map<UUID, Integer> remaining = new LinkedHashMap<>(demand);
        List<Part> parts = new ArrayList<>();
        for (StoreLocation store : byDistance) {
            Map<UUID, Integer> taken = new LinkedHashMap<>();
            remaining.forEach((variant, quantity) -> {
                int take = Math.min(quantity, available(stock, store.storeId(), variant));
                if (take > 0) taken.put(variant, take);
            });
            if (taken.isEmpty()) continue;
            taken.forEach((variant, take) -> remaining.merge(variant, -take, Integer::sum));
            remaining.values().removeIf(quantity -> quantity == 0);
            parts.add(part(store, latitude, longitude, taken));
            if (remaining.isEmpty()) break;
        }
        return new Result(List.copyOf(parts), Map.of());
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    private static Part part(StoreLocation store, double lat, double lon, Map<UUID, Integer> quantities) {
        return new Part(store.storeId(), distanceKm(lat, lon, store.latitude(), store.longitude()), quantities);
    }

    private static int available(Map<UUID, Map<UUID, Integer>> stock, UUID store, UUID variant) {
        return Math.max(0, stock.getOrDefault(store, new HashMap<>()).getOrDefault(variant, 0));
    }
}
