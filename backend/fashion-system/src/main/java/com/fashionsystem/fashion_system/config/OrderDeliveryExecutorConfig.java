package com.fashionsystem.fashion_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Luồng nền gửi email/thông báo của module đơn hàng sau khi transaction commit, để SMTP hoặc Kafka chậm
 * không giữ request của người dùng.
 */
@Configuration
public class OrderDeliveryExecutorConfig {
    public static final String EXECUTOR = "orderDeliveryExecutor";

    @Bean(name = EXECUTOR)
    public TaskExecutor orderDeliveryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("order-delivery-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
