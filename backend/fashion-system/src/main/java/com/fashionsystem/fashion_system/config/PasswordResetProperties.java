package com.fashionsystem.fashion_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "password-reset")
public class PasswordResetProperties {
    private long otpExpirationMinutes;
    private long resetTokenExpirationMinutes;
    private int maxOtpAttempts;

    public long getOtpExpirationMinutes() { return otpExpirationMinutes; }
    public void setOtpExpirationMinutes(long value) { this.otpExpirationMinutes = value; }
    public long getResetTokenExpirationMinutes() { return resetTokenExpirationMinutes; }
    public void setResetTokenExpirationMinutes(long value) { this.resetTokenExpirationMinutes = value; }
    public int getMaxOtpAttempts() { return maxOtpAttempts; }
    public void setMaxOtpAttempts(int value) { this.maxOtpAttempts = value; }
}
