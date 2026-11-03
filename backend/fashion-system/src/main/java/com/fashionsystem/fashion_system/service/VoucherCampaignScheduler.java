package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Gửi các chiến dịch voucher đã tới giờ hẹn; độ trễ tối đa bằng chu kỳ quét (mặc định 1 phút). */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherCampaignScheduler {
    private final VoucherCampaignService campaigns;

    @Scheduled(fixedDelayString = "${voucher-campaign.check-ms:60000}", initialDelayString = "${voucher-campaign.check-ms:60000}")
    public void sendDue() {
        try (var ignored = BusinessAuditContextFactory.systemJob("VOUCHER_CAMPAIGN_SEND")) {
            int sent = campaigns.sendDue(PromotionService.now());
            if (sent > 0) log.info("Voucher campaigns: sent {} scheduled campaign(s)", sent);
        } catch (RuntimeException failure) {
            log.warn("Voucher campaign send failed: {}", failure.getMessage());
        }
    }
}
