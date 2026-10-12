package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.notification.NotificationPublisher;
import com.fashionsystem.fashion_system.notification.NotificationRequest;
import com.fashionsystem.fashion_system.notification.NotificationType;
import com.fashionsystem.fashion_system.notification.PermissionRecipientResolver;
import com.fashionsystem.fashion_system.realtime.RealtimeIdentity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job vòng đời khuyến mãi (giờ Asia/Ho_Chi_Minh):
 * cập nhật SCHEDULED/ACTIVE/EXPIRED mỗi 5 phút và nhắc khuyến mãi sắp hết hạn lúc 08:00 hằng ngày.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionLifecycleScheduler {
    static final String REMINDER_PERMISSION = "PROMOTION_UPDATE";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final PromotionService promotionService;
    private final PermissionRecipientResolver recipientResolver;
    private final NotificationPublisher notificationPublisher;

    @Value("${promotion.expiry-reminder-days:3}")
    private int reminderDays;

    @Scheduled(cron = "${promotion.status-refresh-cron:0 */5 * * * *}", zone = "Asia/Ho_Chi_Minh")
    public void refreshStatuses() {
        try (var ignored = BusinessAuditContextFactory.systemJob("PROMOTION_STATUS_REFRESH")) {
            int changed = promotionService.refreshStatuses(PromotionService.now());
            if (changed > 0) log.info("Promotion status refresh updated {} promotion(s)", changed);
        } catch (RuntimeException failure) {
            log.warn("Promotion status refresh failed", failure);
        }
    }

    @Scheduled(cron = "${promotion.expiry-reminder-cron:0 0 8 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void remindExpiring() {
        LocalDateTime now = PromotionService.now();
        List<Promotion> expiring;
        try {
            expiring = promotionService.findExpiringUnreminded(now, reminderDays);
        } catch (RuntimeException failure) {
            log.warn("Promotion expiry reminder lookup failed", failure);
            return;
        }
        if (expiring.isEmpty()) return;
        Set<RealtimeIdentity> recipients = recipientResolver.resolveGlobalUsers(REMINDER_PERMISSION);
        for (Promotion promotion : expiring) {
            try (var ignored = BusinessAuditContextFactory.systemJob("PROMOTION_EXPIRY_REMINDER")) {
                // Đánh dấu trước khi gửi để không bao giờ gửi trùng cho cùng một ngày kết thúc.
                if (!promotionService.markExpiryReminded(promotion.getId(), now) || recipients.isEmpty()) continue;
                notificationPublisher.publish(new NotificationRequest(
                        List.copyOf(recipients),
                        null,
                        NotificationType.PROMOTION_EXPIRING,
                        "Khuyến mãi sắp hết hạn",
                        "Khuyến mãi " + promotion.getCode() + " - " + promotion.getName()
                                + " sẽ kết thúc lúc " + promotion.getEndDate().format(DATE_TIME) + ".",
                        "PROMOTION",
                        promotion.getId(),
                        "/admin/promotions",
                        Map.of("promotionCode", promotion.getCode(),
                                "endDate", promotion.getEndDate().toString())));
            } catch (RuntimeException failure) {
                log.warn("Promotion expiry reminder failed for {}", promotion.getId(), failure);
            }
        }
    }
}
