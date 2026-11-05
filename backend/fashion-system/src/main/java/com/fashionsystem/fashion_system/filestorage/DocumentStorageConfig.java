package com.fashionsystem.fashion_system.filestorage;

import java.time.Clock;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Chọn nhà cung cấp lưu tài liệu theo {@code document-storage.provider}. Không liên quan tới Cloudinary (ảnh). */
@Slf4j
@Configuration
@EnableConfigurationProperties(DocumentStorageProperties.class)
public class DocumentStorageConfig {
    @Bean
    public DocumentStorage documentStorage(DocumentStorageProperties properties) {
        String provider = properties.getProvider() == null ? "local" : properties.getProvider().trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case "s3", "r2" -> new S3DocumentStorage(properties.getS3());
            case "local" -> {
                log.warn("Document storage is LOCAL ({}). Set DOCUMENT_STORAGE_PROVIDER=s3 with R2/S3 keys in production.",
                        properties.getLocal().getDirectory());
                yield new LocalDocumentStorage(properties.getLocal(), Clock.systemUTC());
            }
            default -> throw new IllegalStateException("document-storage.provider không hỗ trợ: " + provider);
        };
    }
}
