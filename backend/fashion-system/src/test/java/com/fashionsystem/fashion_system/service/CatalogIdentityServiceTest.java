package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;

class CatalogIdentityServiceTest {
    @Test
    void generatesCodesFromDatabaseSequence() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("select nextval('catalog_brand_code_seq')", Long.class)).thenReturn(42L);
        CatalogIdentityService service = new CatalogIdentityService(jdbc);

        assertThat(service.nextBrandCode()).isEqualTo("BR000042");
    }

    @Test
    void generatesVietnameseSlug() {
        CatalogIdentityService service = new CatalogIdentityService(mock(JdbcTemplate.class));
        assertThat(service.slugBase("Áo sơ mi Oxford")).isEqualTo("ao-so-mi-oxford");
    }
}
