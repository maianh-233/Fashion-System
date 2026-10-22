package com.fashionsystem.fashion_system.dto.order;

import com.fashionsystem.fashion_system.entity.ChatSenderType;
import com.fashionsystem.fashion_system.entity.SupportChatStatus;
import com.fashionsystem.fashion_system.entity.SupportTopic;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Request/response của hội thoại hỗ trợ đơn hàng. */
public final class SupportChatDtos {
    private SupportChatDtos() {
    }

    public record Option(String code, String label) {
    }

    public record Message(UUID id, UUID roomId, ChatSenderType senderType, String senderName, String content,
                          List<Option> options, String relatedAction, LocalDateTime createdAt) {
    }

    /**
     * @param staffUnread tin khách chưa được nhân viên đọc
     * @param customerUnread tin nhân viên chưa được khách đọc
     */
    public record Room(UUID id, UUID orderId, String orderCode, String customerName, String customerPhone,
                       SupportChatStatus status, String statusLabel, OrderDetailResponse.Person assignedStaff,
                       int staffUnread, int customerUnread, String lastMessage, LocalDateTime lastMessageAt,
                       LocalDateTime escalatedAt, LocalDateTime createdAt) {
    }

    public record Conversation(Room room, List<Message> messages) {
    }

    /** Khách bấm một nút gợi ý ({@code optionCode}) hoặc tự gõ ({@code text}). */
    public record CustomerSend(@Size(max = 40) String optionCode, @Size(max = 2000) String text) {
    }

    public record StaffReply(@NotBlank(message = "Vui lòng nhập nội dung") @Size(max = 2000) String text) {
    }

    public record Faq(UUID id, SupportTopic topic, String title, String answer, String keywords, int sortOrder,
                      boolean active, LocalDateTime updatedAt) {
    }

    public record FaqUpdate(@NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 200) String title,
                            @NotBlank(message = "Vui lòng nhập nội dung trả lời") @Size(max = 4000) String answer,
                            @Size(max = 500) String keywords, boolean active) {
    }

    public static String statusLabel(SupportChatStatus status) {
        return switch (status) {
            case BOT_HANDLING -> "Bot đang hỗ trợ";
            case WAITING_STAFF -> "Chờ nhân viên";
            case STAFF_HANDLING -> "Nhân viên đang hỗ trợ";
            case CLOSED -> "Đã kết thúc";
        };
    }
}
