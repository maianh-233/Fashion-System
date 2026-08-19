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
 * Entity đại diện cho bảng {@code chat_attachments}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "chat_attachments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAttachment {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã tham chiếu đến message. */
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    /** Lưu giá trị file name của bản ghi. */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /** Lưu giá trị file url của bản ghi. */
    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;

    /** Lưu giá trị file type của bản ghi. */
    @Column(name = "file_type", length = 100)
    private String fileType;

    /** Lưu giá trị file size của bản ghi. */
    @Column(name = "file_size")
    private Long fileSize;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}

