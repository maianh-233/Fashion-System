package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.OrderItem;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho OrderItem.
 */
public interface OrderItemRepository extends BaseRepository<OrderItem, UUID> {
}
