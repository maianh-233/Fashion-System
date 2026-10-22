package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.service.StoreAllocator.StoreLocation;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StoreAllocatorTest {
    final StoreAllocator allocator = new StoreAllocator();
    // Khách ở Quận 1 TP.HCM; cửa hàng gần → xa: Q1, Bình Thạnh, Thủ Đức.
    final double lat = 10.7769, lon = 106.7009;
    final StoreLocation q1 = new StoreLocation(UUID.randomUUID(), "Q1", 10.7731, 106.7030);
    final StoreLocation binhThanh = new StoreLocation(UUID.randomUUID(), "BT", 10.8106, 106.7091);
    final StoreLocation thuDuc = new StoreLocation(UUID.randomUUID(), "TD", 10.8494, 106.7537);
    final List<StoreLocation> stores = List.of(thuDuc, binhThanh, q1);
    final UUID shirt = UUID.randomUUID(), jeans = UUID.randomUUID();

    @Test void haversineDistanceIsRealistic() {
        // Q1 → Thủ Đức khoảng 9-10 km theo đường chim bay.
        assertThat(StoreAllocator.distanceKm(lat, lon, thuDuc.latitude(), thuDuc.longitude())).isCloseTo(9.8, within(0.6));
    }

    @Test void nearestStoreWithFullStockTakesWholeOrder() {
        var result = allocator.allocate(lat, lon, Map.of(shirt, 2, jeans, 1), stores, Map.of(
                q1.storeId(), Map.of(shirt, 2, jeans, 1),
                binhThanh.storeId(), Map.of(shirt, 9, jeans, 9)));
        assertThat(result.split()).isFalse();
        assertThat(result.parts().getFirst().storeId()).isEqualTo(q1.storeId());
    }

    @Test void skipsNearestStoreWhenItCannotCoverTheCart() {
        var result = allocator.allocate(lat, lon, Map.of(shirt, 2, jeans, 1), stores, Map.of(
                q1.storeId(), Map.of(shirt, 2),
                binhThanh.storeId(), Map.of(shirt, 1),
                thuDuc.storeId(), Map.of(shirt, 5, jeans, 5)));
        assertThat(result.split()).isFalse();
        assertThat(result.parts().getFirst().storeId()).isEqualTo(thuDuc.storeId());
    }

    @Test void splitsNearestFirstWhenNoSingleStoreCovers() {
        var result = allocator.allocate(lat, lon, Map.of(shirt, 3, jeans, 2), stores, Map.of(
                q1.storeId(), Map.of(shirt, 2, jeans, 2),
                binhThanh.storeId(), Map.of(shirt, 0),
                thuDuc.storeId(), Map.of(shirt, 4)));
        assertThat(result.split()).isTrue();
        assertThat(result.parts()).hasSize(2);
        assertThat(result.parts().get(0).storeId()).isEqualTo(q1.storeId());
        assertThat(result.parts().get(0).quantities()).containsEntry(shirt, 2).containsEntry(jeans, 2);
        assertThat(result.parts().get(1).storeId()).isEqualTo(thuDuc.storeId());
        assertThat(result.parts().get(1).quantities()).containsExactly(Map.entry(shirt, 1));
    }

    @Test void reportsShortageWhenAllStoresTogetherAreNotEnough() {
        var result = allocator.allocate(lat, lon, Map.of(shirt, 10, jeans, 1), stores, Map.of(
                q1.storeId(), Map.of(shirt, 3, jeans, 1), thuDuc.storeId(), Map.of(shirt, 4)));
        assertThat(result.fulfilled()).isFalse();
        assertThat(result.parts()).isEmpty();
        assertThat(result.shortages()).containsExactly(Map.entry(shirt, 3));
    }
}
