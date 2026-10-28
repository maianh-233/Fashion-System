package com.fashionsystem.fashion_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Định kỳ báo khách khi biến thể họ chờ có hàng online trở lại. Quét theo lịch thay vì móc vào từng nghiệp vụ
 * làm tăng tồn (nhập kho, hủy đơn, chuyển kênh...), nên không bỏ sót đường nào; độ trễ tối đa bằng chu kỳ quét.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockAlertScheduler {
    private final StockAlertService alerts;

    @Scheduled(fixedDelayString = "${stock-alert.check-ms:120000}", initialDelayString = "${stock-alert.check-ms:120000}")
    public void notifyRestocked() {
        try {
            int sent = alerts.notifyRestocked();
            if (sent > 0) log.info("Back-in-stock: notified {} subscription(s)", sent);
        } catch (RuntimeException failure) {
            log.warn("Back-in-stock check failed: {}", failure.getMessage());
        }
    }
}
