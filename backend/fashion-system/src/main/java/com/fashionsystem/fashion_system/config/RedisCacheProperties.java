package com.fashionsystem.fashion_system.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Centralized operational and TTL settings for the shared Redis cache layer. */
@Validated
@ConfigurationProperties(prefix = "redis.cache")
public class RedisCacheProperties {

    private boolean enabled;
    @NotBlank
    private String keyPrefix = "fs:local:v1:";
    @NotNull
    private Duration defaultTtl = Duration.ofMinutes(5);
    @NotNull
    private Duration activeModulesTtl = Duration.ofMinutes(10);
    @NotNull
    private Duration authorizationCatalogTtl = Duration.ofMinutes(5);
    @NotNull
    private Duration authorizationRoleTtl = Duration.ofMinutes(2);
    @NotNull
    private Duration authorizationEffectivePermissionTtl = Duration.ofSeconds(30);
    @NotNull
    private Duration authorizationUserAssignmentTtl = Duration.ofMinutes(1);
    @NotNull
    private Duration catalogTtl = Duration.ofMinutes(10);
    @NotNull
    private Duration productTtl = Duration.ofMinutes(5);
    @NotNull
    private Duration promotionTtl = Duration.ofMinutes(1);
    @NotNull
    private Duration warningInterval = Duration.ofSeconds(30);
    @Min(1)
    private int scanBatchSize = 1_000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }
    public Duration getDefaultTtl() { return defaultTtl; }
    public void setDefaultTtl(Duration defaultTtl) { this.defaultTtl = defaultTtl; }
    public Duration getActiveModulesTtl() { return activeModulesTtl; }
    public void setActiveModulesTtl(Duration activeModulesTtl) { this.activeModulesTtl = activeModulesTtl; }
    public Duration getAuthorizationCatalogTtl() { return authorizationCatalogTtl; }
    public void setAuthorizationCatalogTtl(Duration authorizationCatalogTtl) { this.authorizationCatalogTtl = authorizationCatalogTtl; }
    public Duration getAuthorizationRoleTtl() { return authorizationRoleTtl; }
    public void setAuthorizationRoleTtl(Duration authorizationRoleTtl) { this.authorizationRoleTtl = authorizationRoleTtl; }
    public Duration getAuthorizationEffectivePermissionTtl() { return authorizationEffectivePermissionTtl; }
    public void setAuthorizationEffectivePermissionTtl(Duration authorizationEffectivePermissionTtl) { this.authorizationEffectivePermissionTtl = authorizationEffectivePermissionTtl; }
    public Duration getAuthorizationUserAssignmentTtl() { return authorizationUserAssignmentTtl; }
    public void setAuthorizationUserAssignmentTtl(Duration authorizationUserAssignmentTtl) { this.authorizationUserAssignmentTtl = authorizationUserAssignmentTtl; }
    public Duration getCatalogTtl() { return catalogTtl; }
    public void setCatalogTtl(Duration catalogTtl) { this.catalogTtl = catalogTtl; }
    public Duration getProductTtl() { return productTtl; }
    public void setProductTtl(Duration productTtl) { this.productTtl = productTtl; }
    public Duration getPromotionTtl() { return promotionTtl; }
    public void setPromotionTtl(Duration promotionTtl) { this.promotionTtl = promotionTtl; }
    public Duration getWarningInterval() { return warningInterval; }
    public void setWarningInterval(Duration warningInterval) { this.warningInterval = warningInterval; }
    public int getScanBatchSize() { return scanBatchSize; }
    public void setScanBatchSize(int scanBatchSize) { this.scanBatchSize = scanBatchSize; }
}
