package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Hội thoại hỗ trợ của khách về một đơn (đơn khách nhìn thấy, tức đơn tổng nếu đơn bị tách). */
@Entity
@Table(name = "order_chat_rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /** Nhân viên đang nhận hội thoại. */
    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SupportChatStatus status;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    /** Lúc bot chuyển cho nhân viên (để ưu tiên hội thoại chờ lâu). */
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Builder.Default
    @Column(name = "staff_unread_count", nullable = false)
    private Integer staffUnreadCount = 0;

    @Builder.Default
    @Column(name = "customer_unread_count", nullable = false)
    private Integer customerUnreadCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;
}
