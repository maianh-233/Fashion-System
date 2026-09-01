package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.OrderStatusHistory;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho OrderStatusHistory.
 */
public interface OrderStatusHistoryRepository extends BaseRepository<OrderStatusHistory, UUID> {
    Page<OrderStatusHistory> findAllByOrderId(UUID orderId, Pageable pageable);
}
