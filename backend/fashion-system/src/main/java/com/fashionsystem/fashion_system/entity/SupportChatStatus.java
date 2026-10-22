package com.fashionsystem.fashion_system.entity;

/** Trạng thái hội thoại hỗ trợ đơn hàng. */
public enum SupportChatStatus {
    /** Bot đang trả lời theo kịch bản. */
    BOT_HANDLING,
    /** Khách cần gặp nhân viên, chưa ai nhận. */
    WAITING_STAFF,
    /** Một nhân viên đang trò chuyện với khách. */
    STAFF_HANDLING,
    /** Đã kết thúc; khách nhắn tiếp thì bot mở lại. */
    CLOSED
}
