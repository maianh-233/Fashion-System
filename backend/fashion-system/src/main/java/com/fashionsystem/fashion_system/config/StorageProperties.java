package com.fashionsystem.fashion_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/** Environment-backed settings for the shared Cloudinary storage adapter. */
@ConfigurationProperties(prefix = "storage.cloudinary")
public class StorageProperties {
    private String cloudName = "";
    private String apiKey = "";
    private String apiSecret = "";
    private DataSize maxFileSize = DataSize.ofMegabytes(25);

    public String getCloudName() { return cloudName; }
    public void setCloudName(String cloudName) { this.cloudName = cloudName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    public DataSize getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(DataSize maxFileSize) { this.maxFileSize = maxFileSize; }
}
