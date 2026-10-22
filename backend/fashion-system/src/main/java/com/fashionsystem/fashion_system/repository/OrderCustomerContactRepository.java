package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.OrderCustomerContact;
import java.util.List;
import java.util.UUID;

public interface OrderCustomerContactRepository extends BaseRepository<OrderCustomerContact, UUID> {
    List<OrderCustomerContact> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
