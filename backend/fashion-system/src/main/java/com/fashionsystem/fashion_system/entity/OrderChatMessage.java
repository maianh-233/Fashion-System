package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Tin nhắn trong hội thoại hỗ trợ. */
@Entity
@Table(name = "order_chat_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    /** customers.id với CUSTOMER, users.id với STAFF, null với BOT/SYSTEM. */
    @Column(name = "sender_id")
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private ChatSenderType senderType;

    /** TEXT; OPTIONS khi bot kèm nút gợi ý. */
    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Mã nút khách bấm (ORDER_STATUS, TALK_TO_STAFF...) hoặc hành động hệ thống. */
    @Column(name = "related_action", length = 50)
    private String relatedAction;

    /** JSON các nút gợi ý của bot. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
