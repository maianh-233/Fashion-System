package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductVariantMapper;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cung cấp nghiệp vụ quản lý biến thể thuộc một sản phẩm. */
@Service
@RequiredArgsConstructor
public class ProductVariantService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "sku", "color", "size", "price", "salePrice", "weight", "barcode",
            "active", "createdAt", "updatedAt");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantMapper variantMapper;

    /**
     * Tạo biến thể mới cho một sản phẩm.
     */
    @Transactional
    public ProductVariantDto create(UUID productId, ProductVariantDto request) {
        requireProduct(productId);
        validatePrices(request);
        ensureSkuAvailable(request.getSku(), null);
        ProductVariant entity = variantMapper.toEntity(request);
        entity.setProductId(productId);
        return variantMapper.toDto(variantRepository.save(entity));
    }

    /**
     * Lấy chi tiết biến thể thuộc một sản phẩm.
     */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.PRODUCT_VARIANT_DETAIL,
            key = "#productId + ':' + #variantId")
    public ProductVariantDto getById(UUID productId, UUID variantId) {
        requireProduct(productId);
        return variantMapper.toDto(requireVariant(productId, variantId));
    }

    /**
     * Lấy danh sách biến thể của sản phẩm với tìm kiếm, lọc và phân trang.
     */
    @Transactional(readOnly = true)
    public Page<ProductVariantDto> getList(
            UUID productId,
            String keyword,
            String color,
            String size,
            Boolean active,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        requireProduct(productId);
        validatePriceRange(minPrice, maxPrice);
        validateSort(pageable);
        return variantRepository.searchByProduct(
                        productId, trimToEmpty(keyword), trimToEmpty(color), trimToEmpty(size),
                        active, minPrice, maxPrice, pageable)
                .map(variantMapper::toDto);
    }

    /**
     * Cập nhật biến thể thuộc một sản phẩm.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_VARIANT_DETAIL,
            key = "#productId + ':' + #variantId")
    public ProductVariantDto update(UUID productId, UUID variantId, ProductVariantDto request) {
        requireProduct(productId);
        ProductVariant entity = requireVariant(productId, variantId);
        validatePrices(request);
        ensureSkuAvailable(request.getSku(), variantId);
        variantMapper.updateEntity(request, entity);
        return variantMapper.toDto(variantRepository.save(entity));
    }

    /**
     * Xóa biến thể nếu chưa được dữ liệu kho, đơn hàng hoặc hình ảnh tham chiếu.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheNames.PRODUCT_VARIANT_DETAIL,
            key = "#productId + ':' + #variantId")
    public void delete(UUID productId, UUID variantId) {
        requireProduct(productId);
        ProductVariant entity = requireVariant(productId, variantId);
        try {
            variantRepository.delete(entity);
            variantRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.invalidState("Không thể xóa biến thể sản phẩm đang được sử dụng");
        }
    }

    /**
     * Kích hoạt một biến thể sản phẩm.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_VARIANT_DETAIL,
            key = "#productId + ':' + #variantId")
    public ProductVariantDto activate(UUID productId, UUID variantId) {
        ProductVariant entity = requireVariant(productId, variantId);
        entity.setActive(Boolean.TRUE);
        entity.setUpdatedAt(LocalDateTime.now());
        return variantMapper.toDto(variantRepository.save(entity));
    }

    /**
     * Vô hiệu hóa một biến thể sản phẩm.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_VARIANT_DETAIL,
            key = "#productId + ':' + #variantId")
    public ProductVariantDto deactivate(UUID productId, UUID variantId) {
        ProductVariant entity = requireVariant(productId, variantId);
        entity.setActive(Boolean.FALSE);
        entity.setUpdatedAt(LocalDateTime.now());
        return variantMapper.toDto(variantRepository.save(entity));
    }

    private void requireProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw BusinessException.notFound("Sản phẩm không tồn tại");
        }
    }

    private ProductVariant requireVariant(UUID productId, UUID variantId) {
        return variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> BusinessException.notFound("Biến thể sản phẩm không tồn tại"));
    }

    private void ensureSkuAvailable(String sku, UUID excludedId) {
        String normalized = sku.trim().toUpperCase(Locale.ROOT);
        boolean exists = excludedId == null
                ? variantRepository.existsBySku(normalized)
                : variantRepository.existsBySkuAndIdNot(normalized, excludedId);
        if (exists) throw BusinessException.conflict("SKU đã tồn tại");
    }

    private void validatePrices(ProductVariantDto request) {
        if (request.getPrice() == null || request.getPrice().signum() < 0) {
            throw BusinessException.badRequest("Giá bán phải lớn hơn hoặc bằng 0");
        }
        if (request.getSalePrice() != null
                && (request.getSalePrice().signum() < 0
                || request.getSalePrice().compareTo(request.getPrice()) > 0)) {
            throw BusinessException.badRequest("Giá khuyến mãi phải từ 0 đến giá bán");
        }
        if (request.getWeight() != null && request.getWeight().signum() < 0) {
            throw BusinessException.badRequest("Khối lượng không được âm");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw BusinessException.badRequest("Khoảng giá không hợp lệ");
        }
    }

    private void validateSort(Pageable pageable) {
        boolean invalid = pageable.getSort().stream()
                .anyMatch(order -> !ALLOWED_SORT_FIELDS.contains(order.getProperty()));
        if (invalid) throw BusinessException.badRequest("Trường sắp xếp biến thể không hợp lệ");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
