package com.fashionsystem.fashion_system.marketing;

import com.fashionsystem.fashion_system.audit.BusinessAuditContextFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job gửi đợt thông báo marketing: bắt đầu đợt tới giờ → gửi theo lô tới khi hết việc hoặc hết thời gian của lượt →
 * kết thúc đợt xong → báo đợt chạy quá lâu. Mỗi bước là một giao dịch ngắn trong {@link MarketingDispatchWorker}.
 */
@Slf4j
@Component
class MarketingDispatchScheduler {
    private final MarketingDispatchWorker worker;
    private final MarketingDispatchDelivery delivery;
    private final int batchSize;
    private final long maxRunMs;
    private final int maxAttempts;
    private final int leaseMinutes;
    private final int frequencyCapHours;
    private final int stallMinutes;

    MarketingDispatchScheduler(MarketingDispatchWorker worker, MarketingDispatchDelivery delivery,
                               @Value("${marketing-dispatch.batch-size:200}") int batchSize,
                               @Value("${marketing-dispatch.max-run-ms:20000}") long maxRunMs,
                               @Value("${marketing-dispatch.max-attempts:4}") int maxAttempts,
                               @Value("${marketing-dispatch.lease-minutes:5}") int leaseMinutes,
                               @Value("${marketing-dispatch.frequency-cap-hours:24}") int frequencyCapHours,
                               @Value("${marketing-dispatch.stall-minutes:60}") int stallMinutes) {
        this.worker = worker;
        this.delivery = delivery;
        this.batchSize = batchSize;
        this.maxRunMs = maxRunMs;
        this.maxAttempts = maxAttempts;
        this.leaseMinutes = leaseMinutes;
        this.frequencyCapHours = frequencyCapHours;
        this.stallMinutes = stallMinutes;
    }

    @Scheduled(fixedDelayString = "${marketing-dispatch.check-ms:30000}", initialDelayString = "${marketing-dispatch.check-ms:30000}")
    public void tick() {
        try (var ignored = BusinessAuditContextFactory.systemJob("MARKETING_DISPATCH")) {
            runOnce();
        } catch (RuntimeException failure) {
            log.warn("Marketing dispatch run failed: {}", failure.getMessage());
        }
    }

    /** Một lượt chạy; tách riêng để test gọi trực tiếp. @return số người nhận đã xử lý */
    int runOnce() {
        worker.startDue(LocalDateTime.now(), frequencyCapHours);
        long deadline = System.currentTimeMillis() + maxRunMs;
        int handled = 0;
        while (System.currentTimeMillis() < deadline) {
            List<MarketingDispatchWorker.Work> batch = worker.claimBatch(LocalDateTime.now(), batchSize, leaseMinutes);
            if (batch.isEmpty()) break;
            List<MarketingDispatchWorker.Result> inApp = new ArrayList<>();
            for (MarketingDispatchWorker.Work work : batch) {
                MarketingDispatchWorker.Result result = delivery.deliverOne(work);
                // Email không chống trùng được ở SMTP: ghi kết quả ngay sau từng email để thu hẹp khoảng có thể gửi lặp
                // khi instance chết giữa lô. Thông báo trong app đã chống trùng bằng event_id nên ghi theo lô.
                if (MarketingDispatchService.EMAIL.equals(work.channel())) {
                    worker.recordResults(List.of(result), LocalDateTime.now(), maxAttempts);
                } else {
                    inApp.add(result);
                }
            }
            if (!inApp.isEmpty()) worker.recordResults(inApp, LocalDateTime.now(), maxAttempts);
            handled += batch.size();
        }
        worker.finishDone();
        worker.reportStalled(LocalDateTime.now(), stallMinutes);
        return handled;
    }
}
