package com.fashionsystem.fashion_system.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Cột {@code timestamp} lưu giờ UTC (Hibernate {@code jdbc.time_zone: UTC}); JPA tự đổi về giờ của ứng dụng,
 * còn truy vấn JDBC thì phải đổi bằng tay, nếu không giờ hiển thị lệch múi giờ (VN: sớm 7 tiếng).
 */
public final class DbTimes {
    private DbTimes() {
    }

    /** Giá trị đọc bằng JDBC (UTC) → giờ của ứng dụng, như entity JPA trả về. */
    public static LocalDateTime fromDb(Object value) {
        LocalDateTime utc = value instanceof Timestamp ts ? ts.toLocalDateTime() : (LocalDateTime) value;
        return utc == null ? null
                : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    /** Giờ của ứng dụng → giá trị UTC để ghi bằng JDBC. */
    public static LocalDateTime toDb(LocalDateTime appTime) {
        return appTime == null ? null
                : appTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
