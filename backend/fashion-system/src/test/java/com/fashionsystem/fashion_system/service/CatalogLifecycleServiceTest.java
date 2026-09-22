package com.fashionsystem.fashion_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.dto.ProductDto;
import com.fashionsystem.fashion_system.entity.Brand;
import com.fashionsystem.fashion_system.entity.Product;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.BrandMapper;
import com.fashionsystem.fashion_system.mapper.ProductMapper;
import com.fashionsystem.fashion_system.repository.BrandRepository;
import com.fashionsystem.fashion_system.repository.CategoryRepository;
import com.fashionsystem.fashion_system.repository.CollectionRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogLifecycleServiceTest {
    @Test
    void brandCodeIsAllocatedByServerEvenWhenClientSendsCode() {
        BrandRepository brands = mock(BrandRepository.class);
        CatalogIdentityService identity = mock(CatalogIdentityService.class);
        when(identity.nextBrandCode()).thenReturn("BR000042");
        when(brands.saveAndFlush(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BrandService service = new BrandService(brands, new BrandMapper(), identity,
                mock(ProductRepository.class), mock(ProductVariantRepository.class));

        BrandDto created = service.create(BrandDto.builder().name("Nike").code("CLIENT_CODE").build());

        assertThat(created.getCode()).isEqualTo("BR000042");
    }

    @Test
    void brandDeletionArchivesProductsAndVariantsWithoutPhysicalDelete() {
        UUID id = UUID.randomUUID();
        Brand brand = Brand.builder().id(id).name("Nike").status("ACTIVE").build();
        BrandRepository brands = mock(BrandRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        ProductVariantRepository variants = mock(ProductVariantRepository.class);
        when(brands.findById(id)).thenReturn(Optional.of(brand));
        BrandService service = new BrandService(brands, new BrandMapper(),
                mock(CatalogIdentityService.class), products, variants);

        service.delete(id);

        assertThat(brand.getStatus()).isEqualTo("INACTIVE");
        verify(products).archiveByBrandId(id);
        verify(variants).deactivateByBrandId(id);
        verify(brands, never()).delete(any(Brand.class));
    }

    @Test
    void productRestoreRejectsInactiveBrand() {
        UUID id = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        Product product = Product.builder().id(id).name("Shirt").brandId(brandId)
                .status("ARCHIVE").imageUrl("").build();
        ProductRepository products = mock(ProductRepository.class);
        BrandRepository brands = mock(BrandRepository.class);
        when(products.findById(id)).thenReturn(Optional.of(product));
        when(brands.findById(brandId)).thenReturn(Optional.of(
                Brand.builder().id(brandId).name("Nike").status("INACTIVE").build()));
        ProductService service = new ProductService(products, brands,
                mock(CollectionRepository.class), mock(CategoryRepository.class), new ProductMapper(),
                mock(ProductVariantRepository.class), mock(CatalogIdentityService.class));

        assertThatThrownBy(() -> service.restore(id)).isInstanceOf(BusinessException.class)
                .hasMessageContaining("thương hiệu");
        verify(products, never()).save(any(Product.class));
    }
}
