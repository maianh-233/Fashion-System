package com.fashionsystem.fashion_system.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import com.fashionsystem.fashion_system.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

/** Product image content belongs to ProductImage/Cloudinary and is optional on Product master. */
class ProductDtoValidationTest {
    @Test
    void productMayBeCreatedBeforeAnyVariantImageIsUploaded() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            ProductDto request = ProductDto.builder().name("Áo sơ mi chưa có ảnh").build();
            assertThat(factory.getValidator().validate(request)).isEmpty();
            assertThat(new ProductMapper().toEntity(request).getImageUrl()).isEmpty();
        }
    }
}
