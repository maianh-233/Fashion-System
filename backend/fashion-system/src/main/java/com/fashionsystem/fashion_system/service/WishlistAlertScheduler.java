package com.fashionsystem.fashion_system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Định kỳ báo khi sản phẩm yêu thích giảm giá, vào flash sale hoặc sắp hết hàng. Quét theo lịch thay vì móc vào từng
 * nghiệp vụ đổi giá / tồn / flash sale, nên không bỏ sót đường nào; độ trễ tối đa bằng chu kỳ quét.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WishlistAlertScheduler {
    private final WishlistAlertService alerts;

    @Scheduled(fixedDelayString = "${wishlist-alert.check-ms:300000}", initialDelayString = "${wishlist-alert.check-ms:300000}")
    public void scan() {
        try {
            int sent = alerts.scan();
            if (sent > 0) log.info("Wishlist alerts: notified {} wishlisted product(s)", sent);
        } catch (RuntimeException failure) {
            log.warn("Wishlist alert scan failed: {}", failure.getMessage());
        }
    }
}
