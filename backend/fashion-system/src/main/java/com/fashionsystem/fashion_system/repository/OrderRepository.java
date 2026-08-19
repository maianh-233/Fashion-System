package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Order;
import java.util.UUID;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Order.
 */
public interface OrderRepository extends BaseRepository<Order, UUID> {
}
