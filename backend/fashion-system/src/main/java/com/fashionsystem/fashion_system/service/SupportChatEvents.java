package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.order.SupportChatDtos;
import com.fashionsystem.fashion_system.entity.ChatSenderType;
import java.util.List;
import java.util.UUID;

/** Sự kiện hội thoại hỗ trợ; listener đẩy realtime và tạo thông báo sau khi commit. */
public final class SupportChatEvents {
    private SupportChatEvents() {
    }

    /**
     * Có tin mới trong hội thoại.
     *
     * @param escalated   bot vừa chuyển hội thoại cho nhân viên
     * @param firstUnread tin đầu tiên khách gửi mà nhân viên chưa đọc (chỉ báo một lần cho mỗi đợt)
     */
    public record MessageCreated(UUID roomId, UUID orderId, String orderCode, UUID customerId, UUID assignedStaffId,
                                 List<UUID> storeIds, ChatSenderType senderType, SupportChatDtos.Message message,
                                 SupportChatDtos.Room room, boolean escalated, boolean firstUnread) {
    }
}
