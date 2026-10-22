package com.fashionsystem.fashion_system.notification;

import java.util.UUID;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final NotificationRepository repository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> history(RealtimeIdentity identity, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_SIZE, size <= 0 ? DEFAULT_SIZE : size);
        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findByRecipientTypeAndRecipientId(
                        identity.accountType().name(), identity.accountId(), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(RealtimeIdentity identity) {
        return repository.countByRecipientTypeAndRecipientIdAndReadAtIsNull(
                identity.accountType().name(), identity.accountId());
    }

    /** Chỉ người nhận mới đánh dấu được thông báo của mình; thông báo của người khác trả về 404. */
    @Transactional
    public NotificationResponse markRead(RealtimeIdentity identity, UUID id) {
        var notification = repository.findByIdAndRecipientTypeAndRecipientId(
                        id, identity.accountType().name(), identity.accountId())
                .orElseThrow(() -> com.fashionsystem.fashion_system.exception.BusinessException.notFound(
                        "Thông báo không tồn tại"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(java.time.LocalDateTime.now());
            repository.save(notification);
        }
        return NotificationResponse.from(notification);
    }

    @Transactional
    public int markAllRead(RealtimeIdentity identity) {
        return repository.markAllRead(identity.accountType().name(), identity.accountId(),
                java.time.LocalDateTime.now());
    }
}
