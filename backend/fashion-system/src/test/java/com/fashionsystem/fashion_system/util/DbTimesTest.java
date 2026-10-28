package com.fashionsystem.fashion_system.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DbTimesTest {
    private TimeZone original;

    @BeforeEach
    void vietnam() {
        original = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @AfterEach
    void restore() {
        TimeZone.setDefault(original);
    }

    @Test
    void convertsUtcColumnsToAppTimeAndBack() {
        LocalDateTime utc = LocalDateTime.of(2026, 10, 28, 14, 16);
        assertThat(DbTimes.fromDb(Timestamp.valueOf(utc))).isEqualTo(LocalDateTime.of(2026, 10, 28, 21, 16));
        assertThat(DbTimes.fromDb(utc)).isEqualTo(LocalDateTime.of(2026, 10, 28, 21, 16));
        assertThat(DbTimes.toDb(LocalDateTime.of(2026, 10, 28, 21, 16))).isEqualTo(utc);
        assertThat(DbTimes.fromDb(null)).isNull();
        assertThat(DbTimes.toDb(null)).isNull();
    }
}
