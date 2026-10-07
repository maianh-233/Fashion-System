package com.fashionsystem.fashion_system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Sinh mã Customer duy nhất từ sequence phía server. */
@Component
@RequiredArgsConstructor
public class CustomerCodeGenerator {
    private final JdbcTemplate jdbcTemplate;

    public String nextCode() {
        Long value = jdbcTemplate.queryForObject("select nextval('customer_code_seq')", Long.class);
        if (value == null) throw new IllegalStateException("Không thể sinh mã khách hàng");
        return "CUS%06d".formatted(value);
    }
}

