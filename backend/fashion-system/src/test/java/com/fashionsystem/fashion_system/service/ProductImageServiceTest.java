package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.ProductImageDto;
import com.fashionsystem.fashion_system.dto.StorageUploadResult;
import com.fashionsystem.fashion_system.entity.ProductImage;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.mapper.ProductImageMapper;
import com.fashionsystem.fashion_system.repository.ProductImageRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;

/** Catches persistence of client-owned image metadata and missing Cloudinary compensation. */
class ProductImageServiceTest {
    private ProductVariantRepository variantRepository;
    private ProductImageRepository imageRepository;
    private StorageService storageService;
    private ProductImageService service;
    private UUID productId;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        variantRepository = mock(ProductVariantRepository.class);
        imageRepository = mock(ProductImageRepository.class);
        storageService = mock(StorageService.class);
        service = new ProductImageService(
                variantRepository, imageRepository, new ProductImageMapper(), storageService);
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        when(variantRepository.findByIdForUpdate(variantId)).thenReturn(Optional.of(
                ProductVariant.builder().id(variantId).productId(productId).build()));
    }

    @Test
    void uploadPersistsOnlyCloudinaryUrlAndPublicId() {
        var file = new MockMultipartFile("file", "client-name.png", "image/png", new byte[] {1});
        when(storageService.uploadImage(file, "products/" + productId + "/variants/" + variantId))
                .thenReturn(new StorageUploadResult(
                        "https://res.cloudinary.com/demo/image/upload/generated.png",
                        "fashion-system/products/generated"));
        when(imageRepository.save(any(ProductImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductImageDto result = service.upload(productId, variantId, file, true, 3);

        assertThat(result.getImageUrl())
                .isEqualTo("https://res.cloudinary.com/demo/image/upload/generated.png");
        assertThat(result.getCloudinaryPublicId()).isEqualTo("fashion-system/products/generated");
        ArgumentCaptor<ProductImage> saved = ArgumentCaptor.forClass(ProductImage.class);
        verify(imageRepository).save(saved.capture());
        assertThat(saved.getValue().getProductVariantId()).isEqualTo(variantId);
        assertThat(saved.getValue().getIsPrimary()).isTrue();
        assertThat(saved.getValue().getSortOrder()).isEqualTo(3);
    }

    @Test
    void uploadRejectsSecondImageBeforeSendingFileToCloudinary() {
        var file = new MockMultipartFile("file", "another.png", "image/png", new byte[] {1});
        when(imageRepository.findAllByProductVariantIdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(variantId))
                .thenReturn(List.of(ProductImage.builder().productVariantId(variantId).build()));

        assertThatThrownBy(() -> service.upload(productId, variantId, file, true, 0))
                .hasMessageContaining("một ảnh");
        verify(storageService, never()).uploadImage(any(), any());
    }

    @Test
    void clearsExistingPrimaryBeforeSavingNewPrimaryImage() {
        var file = new MockMultipartFile("file", "image.png", "image/png", new byte[] {1});
        when(storageService.uploadImage(any(), any())).thenReturn(new StorageUploadResult(
                "https://res.cloudinary.com/demo/image/upload/generated.png", "generated"));
        when(imageRepository.save(any(ProductImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.upload(productId, variantId, file, true, 0);

        InOrder order = inOrder(variantRepository, imageRepository);
        order.verify(variantRepository).findByIdForUpdate(variantId);
        order.verify(imageRepository).clearPrimary(variantId);
        order.verify(imageRepository).save(any(ProductImage.class));
    }

    @Test
    void failedPersistenceDeletesNewCloudinaryAsset() {
        var file = new MockMultipartFile("file", "image.png", "image/png", new byte[] {1});
        when(storageService.uploadImage(any(), any())).thenReturn(new StorageUploadResult(
                "https://res.cloudinary.com/demo/image/upload/generated.png", "generated"));
        when(imageRepository.save(any(ProductImage.class))).thenThrow(new RuntimeException("db unavailable"));

        assertThatThrownBy(() -> service.upload(productId, variantId, file, false, 0))
                .isInstanceOf(RuntimeException.class);

        verify(storageService).deleteImage("generated");
    }

    @Test
    void deleteUsesPersistedPublicIdRatherThanRequestData() {
        UUID imageId = UUID.randomUUID();
        ProductImage image = ProductImage.builder()
                .id(imageId)
                .productVariantId(variantId)
                .cloudinaryPublicId("persisted-id")
                .imageUrl("https://res.cloudinary.com/demo/image/upload/persisted.png")
                .build();
        when(imageRepository.findByIdAndProductVariantId(imageId, variantId))
                .thenReturn(Optional.of(image));

        service.delete(productId, variantId, imageId);

        verify(storageService).deleteImage("persisted-id");
        verify(imageRepository).delete(image);
    }
}
