package com.fashionsystem.fashion_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job chăm sóc khách: phát voucher sinh nhật và nhắc giỏ hàng bị bỏ quên. Cả hai đều idempotent (đã phát / đã nhắc
 * thì bỏ qua), nên quét theo chu kỳ thay vì một giờ cố định: server tắt lúc đến giờ thì lần chạy sau vẫn bù lại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerEngagementScheduler {
    private final BirthdayVoucherService birthdayVouchers;
    private final CartReminderService cartReminders;

    @Scheduled(fixedDelayString = "${birthday-voucher.check-ms:3600000}", initialDelayString = "${birthday-voucher.initial-delay-ms:60000}")
    public void issueBirthdayVouchers() {
        try {
            int issued = birthdayVouchers.issueDue(PromotionService.now());
            if (issued > 0) log.info("Birthday vouchers: issued {} voucher(s)", issued);
        } catch (RuntimeException failure) {
            log.warn("Birthday voucher run failed: {}", failure.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${cart-reminder.check-ms:1800000}", initialDelayString = "${cart-reminder.check-ms:1800000}")
    public void remindIdleCarts() {
        try {
            int sent = cartReminders.remindIdleCarts();
            if (sent > 0) log.info("Cart reminders: reminded {} customer(s)", sent);
        } catch (RuntimeException failure) {
            log.warn("Cart reminder run failed: {}", failure.getMessage());
        }
    }
}
