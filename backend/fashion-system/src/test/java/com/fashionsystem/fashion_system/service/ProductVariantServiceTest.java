package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductVariantMapper;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductVariantServiceTest {
    @Test
    void rejectsSalePriceGreaterThanRegularPriceBeforeSaving() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
        UUID productId = UUID.randomUUID();
        when(productRepository.existsById(productId)).thenReturn(true);
        ProductVariantService service = new ProductVariantService(
                productRepository, variantRepository, mock(ProductVariantMapper.class));
        ProductVariantDto request = ProductVariantDto.builder()
                .sku("SKU-1")
                .price(new BigDecimal("100.00"))
                .salePrice(new BigDecimal("110.00"))
                .build();

        assertThatThrownBy(() -> service.create(productId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Giá khuyến mãi");
        verify(variantRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInvalidPriceRangeBeforeQueryingVariants() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
        UUID productId = UUID.randomUUID();
        when(productRepository.existsById(productId)).thenReturn(true);
        ProductVariantService service = new ProductVariantService(
                productRepository, variantRepository, mock(ProductVariantMapper.class));

        assertThatThrownBy(() -> service.getList(
                        productId, null, null, null, null,
                        new BigDecimal("20"), new BigDecimal("10"),
                        org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Khoảng giá");
        verify(variantRepository, never()).searchByProduct(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
