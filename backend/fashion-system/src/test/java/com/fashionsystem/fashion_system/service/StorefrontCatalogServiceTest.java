package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StorefrontCatalogServiceTest {

    @Test
    void keywordIsLowercasedWithoutVietnameseAccentsOrLikeWildcards() {
        assertThat(StorefrontCatalogService.fold("  Áo Sơ Mi ĐẦM ")).isEqualTo("ao so mi dam");
        assertThat(StorefrontCatalogService.fold("100%_x\\")).isEqualTo("100x");
        assertThat(StorefrontCatalogService.fold(null)).isEmpty();
    }

    @Test
    void sqlFoldingMapsEveryAccentedVowelToOneLetter() {
        String expression = StorefrontCatalogService.folded("p.name");
        String from = expression.split("'")[1];
        String to = expression.split("'")[3];
        assertThat(from).hasSameSizeAs(to);
        for (int i = 0; i < from.length(); i++)
            assertThat(StorefrontCatalogService.fold(String.valueOf(from.charAt(i)))).isEqualTo(String.valueOf(to.charAt(i)));
    }
}
