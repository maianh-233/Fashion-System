package com.fashionsystem.fashion_system.recruitment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Luồng riêng cho email ứng viên: không xếp hàng chung với thông báo đơn hàng / Kafka (Kafka chậm hoặc chưa chạy có thể
 * giữ luồng tới ~60 giây, làm email tới ứng viên bị trễ).
 */
@Configuration
public class RecruitmentMailConfig {
    public static final String EXECUTOR = "recruitmentMailExecutor";

    @Bean(name = EXECUTOR)
    public TaskExecutor recruitmentMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("recruitment-mail-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}
