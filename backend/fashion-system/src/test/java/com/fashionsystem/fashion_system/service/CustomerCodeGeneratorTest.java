package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class CustomerCodeGeneratorTest {
    @Test
    void formatsSequenceAsServerSideCustomerCode() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("select nextval('customer_code_seq')", Long.class)).thenReturn(42L);

        assertThat(new CustomerCodeGenerator(jdbc).nextCode()).isEqualTo("CUS000042");
    }
}

