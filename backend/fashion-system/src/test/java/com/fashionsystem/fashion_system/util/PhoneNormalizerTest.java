package com.fashionsystem.fashion_system.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNormalizerTest {
    private final PhoneNormalizer normalizer = new PhoneNormalizer();

    @ParameterizedTest
    @ValueSource(strings = {"0901234567", "84901234567", "+84901234567", "090 123 4567"})
    void normalizesVietnameseAliasesToE164(String input) {
        assertEquals("+84901234567", normalizer.normalize(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "+12025550123", "09012abc567", "84012345678"})
    void rejectsUnsupportedOrMalformedPhones(String input) {
        assertThrows(IllegalArgumentException.class, () -> normalizer.normalize(input));
    }

    @Test
    void normalizesOptionalBlankPhoneToNull() {
        assertNull(normalizer.normalizeOptional("  "));
    }
}

