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
 * Entity đại diện cho bảng {@code notifications}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã tham chiếu đến recipient. */
    @Column(name = "recipient_id")
    private UUID recipientId;

    /** Lưu giá trị recipient type của bản ghi. */
    @Column(name = "recipient_type", nullable = false, length = 20)
    private String recipientType;

    /** Lưu giá trị channel của bản ghi. */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    /** Lưu giá trị title của bản ghi. */
    @Column(name = "title", length = 255)
    private String title;

    /** Lưu nội dung của bản ghi. */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Lưu giá trị reference type của bản ghi. */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /** Lưu mã tham chiếu đến reference. */
    @Column(name = "reference_id")
    private UUID referenceId;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", length = 20)
    private String status;

    /** Lưu giá trị scheduled at của bản ghi. */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu giá trị sent at của bản ghi. */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

}

