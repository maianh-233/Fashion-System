package com.fashionsystem.fashion_system.attendance;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceReminderScheduler {

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void sendCheckInReminders() {
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void sendCheckOutReminders() {
    }
}
