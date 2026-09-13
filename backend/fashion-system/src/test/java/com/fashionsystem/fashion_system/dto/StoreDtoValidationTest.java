package com.fashionsystem.fashion_system.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StoreDtoValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsFormattedVietnamesePhoneAndValidCoordinates() {
        StoreDto request = validRequest();
        request.setPhone("+84 90 123 4567");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidPhoneOrMissingCoordinates() {
        StoreDto request = validRequest();
        request.setPhone("0123");
        request.setLatitude(null);

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsCoordinatesWithMoreThanSixDecimals() {
        StoreDto request = validRequest();
        request.setLongitude(new BigDecimal("106.1234567"));

        assertFalse(validator.validate(request).isEmpty());
    }

    private StoreDto validRequest() {
        return StoreDto.builder()
                .name("Lunaria Test")
                .phone("0901234567")
                .latitude(new BigDecimal("10.123456"))
                .longitude(new BigDecimal("106.123456"))
                .active(true)
                .build();
    }
}
