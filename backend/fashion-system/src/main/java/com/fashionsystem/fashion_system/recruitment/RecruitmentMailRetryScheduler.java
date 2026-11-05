package com.fashionsystem.fashion_system.recruitment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Gửi lại email ứng viên bị kẹt ở trạng thái "Đang gửi" (ví dụ ứng dụng tắt giữa chừng). */
@Slf4j
@Component
public class RecruitmentMailRetryScheduler {
    private final RecruitmentMailer mailer;

    public RecruitmentMailRetryScheduler(RecruitmentMailer mailer) {
        this.mailer = mailer;
    }

    @Scheduled(fixedDelayString = "${recruitment.mail-retry-check-ms:300000}", initialDelayString = "${recruitment.mail-retry-initial-ms:120000}")
    public void resend() {
        try {
            int sent = mailer.resendStale(10);
            if (sent > 0) log.info("Re-sent {} stale recruitment emails", sent);
        } catch (RuntimeException e) {
            log.error("Recruitment email retry failed", e);
        }
    }
}
