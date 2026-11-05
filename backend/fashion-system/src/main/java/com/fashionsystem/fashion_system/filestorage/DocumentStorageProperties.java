package com.fashionsystem.fashion_system.filestorage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Cấu hình kho tài liệu riêng (CV, thư xin việc, portfolio...). Ảnh vẫn đi qua Cloudinary ({@code storage.cloudinary}).
 * Khóa truy cập chỉ nằm ở backend (biến môi trường), không bao giờ trả về frontend.
 */
@ConfigurationProperties(prefix = "document-storage")
public class DocumentStorageProperties {
    /** {@code s3} (Cloudflare R2, Amazon S3 hoặc dịch vụ tương thích S3) hoặc {@code local} (chỉ dùng khi phát triển). */
    private String provider = "local";
    private DataSize maxFileSize = DataSize.ofMegabytes(10);
    /** Thời hạn của link xem / tải đã ký. */
    private Duration signedUrlTtl = Duration.ofMinutes(5);
    private final S3 s3 = new S3();
    private final Local local = new Local();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public DataSize getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(DataSize maxFileSize) { this.maxFileSize = maxFileSize; }
    public Duration getSignedUrlTtl() { return signedUrlTtl; }
    public void setSignedUrlTtl(Duration signedUrlTtl) { this.signedUrlTtl = signedUrlTtl; }
    public S3 getS3() { return s3; }
    public Local getLocal() { return local; }

    public static class S3 {
        /** R2: https://(account-id).r2.cloudflarestorage.com; để trống với Amazon S3. */
        private String endpoint = "";
        /** R2 dùng {@code auto}. */
        private String region = "auto";
        private String bucket = "";
        private String accessKey = "";
        private String secretKey = "";
        private boolean pathStyle = true;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public boolean isPathStyle() { return pathStyle; }
        public void setPathStyle(boolean pathStyle) { this.pathStyle = pathStyle; }
    }

    public static class Local {
        private String directory = "var/documents";
        /** Khóa HMAC ký link tải của kho local; trống thì sinh ngẫu nhiên mỗi lần khởi động. */
        private String signingKey = "";

        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }
        public String getSigningKey() { return signingKey; }
        public void setSigningKey(String signingKey) { this.signingKey = signingKey; }
    }
}
