package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.OrderChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** Tin nhắn hội thoại hỗ trợ. */
public interface OrderChatMessageRepository extends BaseRepository<OrderChatMessage, UUID> {
    /** Tin mới nhất trước; service đảo lại để hiển thị theo thời gian. */
    List<OrderChatMessage> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);
}
