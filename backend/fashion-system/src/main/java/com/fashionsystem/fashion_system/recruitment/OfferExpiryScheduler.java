package com.fashionsystem.fashion_system.recruitment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Đánh dấu offer đã gửi nhưng quá hạn phản hồi. */
@Slf4j
@Component
public class OfferExpiryScheduler {
    private final OfferService offers;

    public OfferExpiryScheduler(OfferService offers) {
        this.offers = offers;
    }

    @Scheduled(fixedDelayString = "${recruitment.offer-expiry-check-ms:3600000}", initialDelayString = "${recruitment.offer-expiry-initial-ms:60000}")
    public void expire() {
        try {
            int expired = offers.expireOverdue();
            if (expired > 0) log.info("Expired {} recruitment offers past their response deadline", expired);
        } catch (RuntimeException e) {
            log.error("Offer expiry job failed", e);
        }
    }
}
