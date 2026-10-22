package com.fashionsystem.fashion_system.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Sinh mã đơn hàng dạng {@code LB-yyMMdd-000123}.
 *
 * <p>Phần số lấy từ sequence PostgreSQL {@code order_code_seq}: nextval là thao tác nguyên tử và
 * không bị rollback, nên nhiều nhân viên tạo đơn cùng lúc vẫn không bao giờ trùng mã. Phần ngày
 * chỉ để dễ đọc, sequence không đặt lại theo ngày.</p>
 */
@Component
public class OrderCodeGenerator {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    @Autowired
    public OrderCodeGenerator(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Clock.system(ZONE));
    }

    OrderCodeGenerator(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public String nextCode() {
        Long value = jdbcTemplate.queryForObject("select nextval('order_code_seq')", Long.class);
        if (value == null) throw new IllegalStateException("Không thể sinh mã đơn hàng");
        return "LB-%s-%06d".formatted(LocalDate.now(clock.withZone(ZONE)).format(DATE), value);
    }
}
