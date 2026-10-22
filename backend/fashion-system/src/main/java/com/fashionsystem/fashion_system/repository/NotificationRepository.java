package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Notification;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Notification.
 */
public interface NotificationRepository extends BaseRepository<Notification, UUID> {
    Page<Notification> findByRecipientTypeAndRecipientId(
            String recipientType, UUID recipientId, Pageable pageable);

    boolean existsByEventIdAndRecipientTypeAndRecipientId(
            UUID eventId, String recipientType, UUID recipientId);

    long countByRecipientTypeAndRecipientIdAndReadAtIsNull(String recipientType, UUID recipientId);

    java.util.Optional<Notification> findByIdAndRecipientTypeAndRecipientId(
            UUID id, String recipientType, UUID recipientId);

    /** Đánh dấu đã đọc mọi thông báo chưa đọc của một người nhận. */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("""
            update Notification n set n.readAt = :readAt
            where n.recipientType = :recipientType and n.recipientId = :recipientId and n.readAt is null
            """)
    int markAllRead(@org.springframework.data.repository.query.Param("recipientType") String recipientType,
                    @org.springframework.data.repository.query.Param("recipientId") UUID recipientId,
                    @org.springframework.data.repository.query.Param("readAt") java.time.LocalDateTime readAt);
}
