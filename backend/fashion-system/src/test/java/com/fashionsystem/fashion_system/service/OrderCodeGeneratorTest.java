package com.fashionsystem.fashion_system.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderCodeGeneratorTest {
    @Test void formatsVietnamDateAndPaddedSequence() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("select nextval('order_code_seq')", Long.class)).thenReturn(42L);
        // 2026-10-14 18:30 UTC là 01:30 ngày 15/10 giờ Việt Nam.
        Clock clock = Clock.fixed(Instant.parse("2026-10-14T18:30:00Z"), ZoneOffset.UTC);

        assertThat(new OrderCodeGenerator(jdbc, clock).nextCode()).isEqualTo("LB-261015-000042");
    }
}
