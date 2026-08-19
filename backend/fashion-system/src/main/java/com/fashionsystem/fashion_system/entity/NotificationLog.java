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
 * Entity đại diện cho bảng {@code notification_logs}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã tham chiếu đến notification. */
    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    /** Lưu giá trị channel của bản ghi. */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    /** Lưu giá trị destination của bản ghi. */
    @Column(name = "destination", length = 255)
    private String destination;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Lưu giá trị error message của bản ghi. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Lưu giá trị provider của bản ghi. */
    @Column(name = "provider", length = 50)
    private String provider;

    /** Lưu giá trị sent at của bản ghi. */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

}

