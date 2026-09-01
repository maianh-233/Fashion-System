package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.OrderItem;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho OrderItem.
 */
public interface OrderItemRepository extends BaseRepository<OrderItem, UUID> {
    Optional<OrderItem> findByIdAndOrderId(UUID id, UUID orderId);
    List<OrderItem> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);

    @Query("select coalesce(sum(i.total), 0) from OrderItem i where i.orderId = :orderId")
    BigDecimal sumTotal(@Param("orderId") UUID orderId);
}
