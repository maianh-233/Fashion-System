package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.ProductImageDto;
import com.fashionsystem.fashion_system.entity.ProductImage;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.mapper.ProductImageMapper;
import com.fashionsystem.fashion_system.repository.ProductImageRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ProductImageServiceTest {
    @Test
    void clearsExistingPrimaryBeforeCreatingNewPrimaryImage() {
        ProductVariantRepository variantRepository = mock(ProductVariantRepository.class);
        ProductImageRepository imageRepository = mock(ProductImageRepository.class);
        ProductImageMapper imageMapper = mock(ProductImageMapper.class);
        UUID variantId = UUID.randomUUID();
        ProductImageDto request = ProductImageDto.builder()
                .imageUrl("https://cdn.test/product.jpg")
                .isPrimary(true)
                .build();
        ProductImage entity = new ProductImage();
        ProductImageDto response = ProductImageDto.builder().isPrimary(true).build();
        when(variantRepository.findByIdForUpdate(variantId))
                .thenReturn(Optional.of(ProductVariant.builder().id(variantId).build()));
        when(imageMapper.toEntity(request)).thenReturn(entity);
        when(imageRepository.save(entity)).thenReturn(entity);
        when(imageMapper.toDto(entity)).thenReturn(response);
        ProductImageService service = new ProductImageService(
                variantRepository, imageRepository, imageMapper);

        ProductImageDto result = service.create(variantId, request);

        assertThat(result.getIsPrimary()).isTrue();
        assertThat(entity.getProductVariantId()).isEqualTo(variantId);
        InOrder order = inOrder(variantRepository, imageRepository);
        order.verify(variantRepository).findByIdForUpdate(variantId);
        order.verify(imageRepository).clearPrimary(variantId);
        order.verify(imageRepository).save(entity);
    }
}
