package com.fashionsystem.fashion_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "password-reset")
public class PasswordResetProperties {
    private long otpExpirationMinutes;
    private long customerOtpExpirationMinutes;
    private long resetTokenExpirationMinutes;
    private int maxOtpAttempts;
    private long resendCooldownSeconds;

    public long getOtpExpirationMinutes() { return otpExpirationMinutes; }
    public void setOtpExpirationMinutes(long value) { this.otpExpirationMinutes = value; }
    public long getCustomerOtpExpirationMinutes() { return customerOtpExpirationMinutes; }
    public void setCustomerOtpExpirationMinutes(long value) { this.customerOtpExpirationMinutes = value; }
    public long getResetTokenExpirationMinutes() { return resetTokenExpirationMinutes; }
    public void setResetTokenExpirationMinutes(long value) { this.resetTokenExpirationMinutes = value; }
    public int getMaxOtpAttempts() { return maxOtpAttempts; }
    public void setMaxOtpAttempts(int value) { this.maxOtpAttempts = value; }
    public long getResendCooldownSeconds() { return resendCooldownSeconds; }
    public void setResendCooldownSeconds(long value) { this.resendCooldownSeconds = value; }
}
