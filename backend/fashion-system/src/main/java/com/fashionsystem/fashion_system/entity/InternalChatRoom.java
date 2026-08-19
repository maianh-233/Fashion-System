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
 * Entity đại diện cho bảng {@code internal_chat_rooms}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "internal_chat_rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalChatRoom {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã người tạo của bản ghi. */
    @Column(name = "created_by")
    private UUID createdBy;

    /** Lưu giá trị room type của bản ghi. */
    @Column(name = "room_type", nullable = false, length = 20)
    private String roomType;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Lưu giá trị closed at của bản ghi. */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

}

