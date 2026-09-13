package com.fashionsystem.fashion_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private int maxFailedAttempts = 5;
    private long loginLockDurationSeconds = 60;

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public long getLoginLockDurationSeconds() {
        return loginLockDurationSeconds;
    }

    public void setLoginLockDurationSeconds(long loginLockDurationSeconds) {
        this.loginLockDurationSeconds = loginLockDurationSeconds;
    }
}
