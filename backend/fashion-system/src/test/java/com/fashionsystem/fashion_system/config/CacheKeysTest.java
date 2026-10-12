package com.fashionsystem.fashion_system.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class CacheKeysTest {

    @Test
    void distinguishesNullEmptyAndDelimiterLikeValues() {
        assertThat(CacheKeys.of((Object) null)).isNotEqualTo(CacheKeys.of(""));
        assertThat(CacheKeys.of("a|b", "c")).isNotEqualTo(CacheKeys.of("a", "b|c"));
    }

    @Test
    void encodesEveryPageAndSortAttribute() {
        var base = PageRequest.of(0, 20,
                Sort.by(Sort.Order.asc("name").ignoreCase().nullsLast()));

        assertThat(CacheKeys.of(base)).isNotEqualTo(CacheKeys.of(PageRequest.of(1, 20, base.getSort())));
        assertThat(CacheKeys.of(base)).isNotEqualTo(CacheKeys.of(PageRequest.of(0, 10, base.getSort())));
        assertThat(CacheKeys.of(base)).isNotEqualTo(CacheKeys.of(PageRequest.of(0, 20,
                Sort.by(Sort.Order.desc("name").ignoreCase().nullsLast()))));
        assertThat(CacheKeys.of(base)).isNotEqualTo(CacheKeys.of(PageRequest.of(0, 20,
                Sort.by(Sort.Order.asc("name").nullsLast()))));
        assertThat(CacheKeys.of(base)).isNotEqualTo(CacheKeys.of(PageRequest.of(0, 20,
                Sort.by(Sort.Order.asc("name").ignoreCase().nullsFirst()))));
    }

    @Test
    void isDeterministicForSupportedScalarTypes() {
        UUID id = UUID.fromString("3a9eb625-f60d-4bb2-aa44-57d7fca55871");
        var pageable = PageRequest.of(2, 25, Sort.by(
                Sort.Order.desc("updatedAt"), Sort.Order.asc("id")));

        assertThat(CacheKeys.of("active", id, true, 12, pageable))
                .isEqualTo(CacheKeys.of("active", id, true, 12, pageable));
    }

    @Test
    void rejectsUnsupportedMutableValues() {
        assertThatThrownBy(() -> CacheKeys.of(List.of("ACTIVE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported cache key component");
    }
}
