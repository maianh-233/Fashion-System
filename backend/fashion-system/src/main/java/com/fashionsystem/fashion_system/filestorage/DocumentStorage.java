package com.fashionsystem.fashion_system.filestorage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Cổng tới nhà cung cấp lưu trữ tài liệu private. Đổi nhà cung cấp = thêm một implementation, nghiệp vụ không đổi.
 * Mọi object đều private; chỉ đọc được qua link đã ký có thời hạn.
 */
public interface DocumentStorage {
    /** Tên nhà cung cấp lưu cùng metadata ({@code s3}, {@code local}...). */
    String provider();

    void put(String key, InputStream content, long size, String contentType);

    /**
     * Link GET đã ký.
     *
     * @param inline true: mở xem trên trình duyệt; false: tải xuống với tên {@code fileName}
     */
    String signedUrl(String key, Duration ttl, String fileName, String contentType, boolean inline);

    void delete(String key);
}
