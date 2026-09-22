package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.entity.ProductImage;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductVariantMapper;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductImageRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;

class ProductVariantServiceTest {
    @Test
    void rejectsSameColorAndSizeWithinOneProduct() {
        ProductRepository products = mock(ProductRepository.class);
        ProductVariantRepository variants = mock(ProductVariantRepository.class);
        UUID productId = UUID.randomUUID();
        when(products.existsById(productId)).thenReturn(true);
        when(products.findById(productId)).thenReturn(java.util.Optional.of(
                com.fashionsystem.fashion_system.entity.Product.builder().id(productId).status("ACTIVE").build()));
        when(variants.existsCombination(productId, "black", "m", null)).thenReturn(true);
        ProductVariantService service = new ProductVariantService(products, variants,
                new ProductVariantMapper(), mock(CatalogIdentityService.class), mock(ProductImageRepository.class));

        assertThatThrownBy(() -> service.create(productId, ProductVariantDto.builder()
                .color(" Black ").size("M").price(BigDecimal.TEN).build()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã tồn tại");
        verify(variants, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }
    @Test
    void catalogPageIncludesVariantImageWithoutPerRowQueries() {
        ProductRepository products = mock(ProductRepository.class);
        ProductVariantRepository variants = mock(ProductVariantRepository.class);
        ProductImageRepository images = mock(ProductImageRepository.class);
        ProductVariantMapper mapper = new ProductVariantMapper();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = ProductVariant.builder().id(variantId).productId(productId)
                .sku("SKU1").price(BigDecimal.ONE).active(true).build();
        when(variants.searchAll(null, "", "", "", true, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(variant)));
        when(images.findAllByProductVariantIdIn(List.of(variantId))).thenReturn(List.of(
                ProductImage.builder().productVariantId(variantId).imageUrl("https://example.com/image.jpg")
                        .isPrimary(true).build()));
        ProductVariantService service = new ProductVariantService(products, variants, mapper,
                mock(CatalogIdentityService.class), images);

        var page = service.getAll(null, null, null, null, true, Pageable.unpaged());

        org.assertj.core.api.Assertions.assertThat(page.getContent().getFirst().getImageUrl())
                .isEqualTo("https://example.com/image.jpg");
    }
    @Test
    void rejectsSalePriceGreaterThanRegularPriceBeforeSaving() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
        UUID productId = UUID.randomUUID();
        when(productRepository.existsById(productId)).thenReturn(true);
        ProductVariantService service = new ProductVariantService(
                productRepository, variantRepository, mock(ProductVariantMapper.class), mock(CatalogIdentityService.class), mock(ProductImageRepository.class));
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
                productRepository, variantRepository, mock(ProductVariantMapper.class), mock(CatalogIdentityService.class), mock(ProductImageRepository.class));

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
