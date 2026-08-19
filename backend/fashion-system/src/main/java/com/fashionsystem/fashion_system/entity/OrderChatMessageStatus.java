package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code order_chat_message_status}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "order_chat_message_status")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(OrderChatMessageStatusId.class)
public class OrderChatMessageStatus {
    /** Lưu mã tham chiếu đến message. */
    @Id
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    /** Lưu mã người dùng của bản ghi. */
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}

