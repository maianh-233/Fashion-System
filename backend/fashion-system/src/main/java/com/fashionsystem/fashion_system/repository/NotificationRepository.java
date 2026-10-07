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
}
