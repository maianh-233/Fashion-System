package com.fashionsystem.fashion_system.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Định kỳ nhắc công việc sắp đến hạn (trước 24 giờ) và báo công việc quá hạn. */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkTaskReminderScheduler {
    private final WorkTaskService tasks;

    @Scheduled(fixedDelayString = "${work-task.reminder-check-ms:300000}", initialDelayString = "${work-task.reminder-check-ms:300000}")
    public void remind() {
        try {
            int sent = tasks.sendDueReminders();
            if (sent > 0) log.info("Work tasks: sent {} due reminder(s)", sent);
        } catch (RuntimeException failure) {
            log.warn("Work task reminder check failed: {}", failure.getMessage());
        }
    }
}
