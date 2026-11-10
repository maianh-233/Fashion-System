package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.helper.MailHelper;
import com.fashionsystem.fashion_system.marketing.MarketingDispatchWorker.Outcome;
import com.fashionsystem.fashion_system.marketing.MarketingDispatchWorker.Result;
import com.fashionsystem.fashion_system.marketing.MarketingDispatchWorker.Work;
import com.fashionsystem.fashion_system.notification.NotificationEvent;
import com.fashionsystem.fashion_system.notification.NotificationPersistenceService;
import com.fashionsystem.fashion_system.notification.PersistedNotification;
import com.fashionsystem.fashion_system.realtime.RealtimeEvent;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import com.fashionsystem.fashion_system.realtime.RealtimePublisher;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Gửi một người nhận, ngoài mọi giao dịch của job.
 * <ul>
 *   <li>Thông báo trong app: ghi thẳng qua {@link NotificationPersistenceService} (đồng bộ, REQUIRES_NEW, chống trùng theo
 *       event_id + người nhận) thay vì {@code NotificationPublisher} — publisher hiện có sinh event_id ngẫu nhiên, không trả
 *       kết quả và bỏ qua thông báo khi tắt Kafka. "Đã gửi" = đã có dòng trong bảng notifications; đẩy realtime là
 *       best-effort như NotificationConsumer.</li>
 *   <li>Email: chỉ gửi khi khách vẫn còn đồng ý ngay lúc gửi (có thể vừa hủy đăng ký sau khi chốt danh sách); SMTP không
 *       có chống trùng nên gửi theo kiểu ít nhất một lần.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MarketingDispatchDelivery {
    static final String TYPE = "MARKETING_CAMPAIGN";
    static final String ENTITY = "MARKETING_DISPATCH";

    private final NotificationPersistenceService persistence;
    private final RealtimePublisher realtime;
    private final NamedParameterJdbcTemplate jdbc;
    private final MailHelper mail;
    private final MarketingEmailComposer composer;

    Result deliverOne(Work work) {
        if (!work.customerActive()) return skipped(work, "INACTIVE");
        return MarketingDispatchService.EMAIL.equals(work.channel()) ? email(work) : inApp(work);
    }

    private Result inApp(Work work) {
        if (work.inAppOptedOut()) return skipped(work, "OPTED_OUT");
        RealtimeIdentity customer = RealtimeIdentity.customer(work.customerId());
        try {
            NotificationEvent event = new NotificationEvent(1, work.eventId(), List.of(customer), null, TYPE, work.title(),
                    work.message(), ENTITY, work.dispatchId(), work.actionUrl(),
                    Map.of("campaignId", work.campaignId().toString(), "dispatchId", work.dispatchId().toString()), Instant.now());
            List<PersistedNotification> created = persistence.persist(event);
            UUID notificationId = created.isEmpty() ? existing(work) : created.getFirst().notification().id();
            for (PersistedNotification persisted : created) push(persisted);
            return new Result(work.recipientId(), work.attempt(), Outcome.SENT, notificationId, null, null);
        } catch (IllegalArgumentException invalid) {
            return failure(work, Outcome.FAILED, "INVALID_NOTIFICATION", invalid);
        } catch (DataIntegrityViolationException integrity) {
            return failure(work, Outcome.FAILED, "DATA_INTEGRITY", integrity);
        } catch (RuntimeException transientOrUnknown) {
            // Lỗi kết nối / timeout / deadlock hoặc lỗi chưa biết: thử lại có backoff tới khi hết số lần cho phép.
            return failure(work, Outcome.RETRY, transientOrUnknown.getClass().getSimpleName(), transientOrUnknown);
        }
    }

    private Result email(Work work) {
        if (!work.emailConsent() || work.unsubscribeToken() == null) return skipped(work, "NO_CONSENT");
        if (work.email() == null || work.email().isBlank()) return skipped(work, "NO_EMAIL");
        try {
            MarketingEmailComposer.Email email = composer.compose(work.title(), work.message(), work.actionUrl(), work.unsubscribeToken());
            mail.sendHtml(work.email(), email.subject(), email.html());
            return new Result(work.recipientId(), work.attempt(), Outcome.SENT, null, null, null);
        } catch (RuntimeException failure) {
            // Máy chủ SMTP lỗi / timeout: thử lại có backoff; sau số lần tối đa thì FAILED.
            return failure(work, Outcome.RETRY, failure.getClass().getSimpleName(), failure);
        }
    }

    /** Thông báo đã được ghi ở lần trước (sự cố giữa lúc ghi và lúc đánh dấu SENT): lấy lại id, không tạo dòng mới. */
    private UUID existing(Work work) {
        return jdbc.queryForList("""
                select id from notifications where event_id = :event and recipient_type = 'CUSTOMER' and recipient_id = :customer""",
                new MapSqlParameterSource("event", work.eventId()).addValue("customer", work.customerId()), UUID.class)
                .stream().findFirst().orElse(null);
    }

    private void push(PersistedNotification persisted) {
        try {
            realtime.sendToIdentity(persisted.recipient(), new RealtimeEvent<>(1, UUID.randomUUID(), "NOTIFICATION_CREATED", "NOTIFICATION",
                    persisted.recipient().accountType(), persisted.recipient().accountId(), persisted.notification(), Instant.now()));
        } catch (RuntimeException failure) {
            log.warn("Realtime push for marketing notification {} failed: {}", persisted.notification().id(), failure.getMessage());
        }
    }

    private static Result skipped(Work work, String reason) {
        return new Result(work.recipientId(), work.attempt(), Outcome.SKIPPED, null, reason, null);
    }

    private static Result failure(Work work, Outcome outcome, String code, RuntimeException error) {
        log.warn("Marketing {} to customer {} (attempt {}) failed: {}", work.channel(), work.customerId(), work.attempt(), error.getMessage());
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return new Result(work.recipientId(), work.attempt(), outcome, null, code.length() > 50 ? code.substring(0, 50) : code, message);
    }
}
