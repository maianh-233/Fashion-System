package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code internal_chat_messages}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "internal_chat_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalChatMessage {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã tham chiếu đến room. */
    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    /** Lưu mã tham chiếu đến sender. */
    @Column(name = "sender_id")
    private UUID senderId;

    /** Lưu giá trị sender type của bản ghi. */
    @Column(name = "sender_type", length = 20)
    private String senderType;

    /** Lưu nội dung của bản ghi. */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Lưu giá trị intent của bản ghi. */
    @Column(name = "intent", length = 100)
    private String intent;

    /** Lưu dữ liệu bổ sung dạng JSON của bản ghi. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}

