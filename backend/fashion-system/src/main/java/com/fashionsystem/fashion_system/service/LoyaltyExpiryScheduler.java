package com.fashionsystem.fashion_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Hằng ngày: cho hết hạn điểm đã tới hạn (sau nửa đêm) và nhắc khách có điểm sắp hết hạn (buổi sáng). */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoyaltyExpiryScheduler {
    private final LoyaltyExpiryService expiry;

    @Scheduled(cron = "${loyalty.expiry-cron:0 10 0 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void expire() {
        try {
            int customers;
            // Mỗi lượt tối đa một lô khách; chạy tiếp tới khi hết để không dồn sang ngày sau.
            do {
                customers = expiry.expireDue(PromotionService.now());
                if (customers > 0) log.info("Points expiry: expired points of {} customer(s)", customers);
            } while (customers > 0);
        } catch (RuntimeException failure) {
            log.warn("Points expiry failed: {}", failure.getMessage());
        }
    }

    @Scheduled(cron = "${loyalty.expiry-reminder-cron:0 0 9 * * *}", zone = "Asia/Ho_Chi_Minh")
    public void remind() {
        try {
            int sent = expiry.remindDue(PromotionService.now());
            if (sent > 0) log.info("Points expiry: scheduled {} reminder(s)", sent);
        } catch (RuntimeException failure) {
            log.warn("Points expiry reminder failed: {}", failure.getMessage());
        }
    }
}
