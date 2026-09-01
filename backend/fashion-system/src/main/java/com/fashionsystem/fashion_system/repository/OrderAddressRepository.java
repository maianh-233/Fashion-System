package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.OrderAddress;
import java.util.UUID;
import java.util.Optional;

/**
 * Cung cấp các thao tác CRUD cơ bản cho OrderAddress.
 */
public interface OrderAddressRepository extends BaseRepository<OrderAddress, UUID> {
    Optional<OrderAddress> findByOrderId(UUID orderId);
    boolean existsByOrderId(UUID orderId);
}
