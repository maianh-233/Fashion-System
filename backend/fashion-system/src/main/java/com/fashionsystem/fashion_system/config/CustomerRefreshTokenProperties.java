package com.fashionsystem.fashion_system.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "auth.customer-refresh-token")
public class CustomerRefreshTokenProperties {
    private Duration expiration = Duration.ofDays(7);
    private String cookieName = "lunaria_customer_refresh_token";
    private String cookiePath = "/api/auth/customer";
    private boolean secure = true;
    private String sameSite = "Lax";

    public Duration getExpiration() { return expiration; }
    public void setExpiration(Duration expiration) { this.expiration = expiration; }
    public String getCookieName() { return cookieName; }
    public void setCookieName(String cookieName) { this.cookieName = cookieName; }
    public String getCookiePath() { return cookiePath; }
    public void setCookiePath(String cookiePath) { this.cookiePath = cookiePath; }
    public boolean isSecure() { return secure; }
    public void setSecure(boolean secure) { this.secure = secure; }
    public String getSameSite() { return sameSite; }
    public void setSameSite(String sameSite) { this.sameSite = sameSite; }
}

