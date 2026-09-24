package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.ProductVariantDto;
import com.fashionsystem.fashion_system.config.CacheNames;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import com.fashionsystem.fashion_system.entity.ProductImage;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.ProductVariantMapper;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.ProductImageRepository;
import com.fashionsystem.fashion_system.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@com.fashionsystem.fashion_system.audit.BusinessAudit("VARIANT")
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductVariantService {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "productId", "sku", "color", "size", "price", "salePrice", "weight", "barcode",
            "active", "createdAt", "updatedAt");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantMapper variantMapper;
    private final CatalogIdentityService identityService;
    private final ProductImageRepository imageRepository;

    /**
     * Tạo biến thể mới cho một sản phẩm.
     */
    @Transactional
    public ProductVariantDto create(UUID productId, ProductVariantDto request) {
        requireProduct(productId);
        validatePrices(request);
        requireActiveProduct(productId);
        ensureCombinationAvailable(productId, request, null);
        ProductVariant entity = variantMapper.toEntity(request);
        entity.setProductId(productId);
        entity.setSku(identityService.nextSku());
        entity.setActive(true);
        try {
            return variantMapper.toDto(variantRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("SKU hoặc tổ hợp biến thể đã tồn tại");
        }
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
            String variantSize,
            Boolean active,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {
        requireProduct(productId);
        validatePriceRange(minPrice, maxPrice);
        validateSort(pageable);
        Page<ProductVariant> page = variantRepository.searchByProduct(
                        productId, trimToEmpty(keyword), trimToEmpty(color), trimToEmpty(variantSize),
                        active, minPrice, maxPrice, pageable);
        if (page.isEmpty()) {
            log.warn("Variant list empty: productId={}, keyword='{}', color='{}', variantSize='{}', active={}, minPrice={}, maxPrice={}, page={}, size={}, variantsForProduct={}",
                    productId, trimToEmpty(keyword), trimToEmpty(color), trimToEmpty(variantSize), active,
                    minPrice, maxPrice,
                    pageable.isPaged() ? pageable.getPageNumber() : null,
                    pageable.isPaged() ? pageable.getPageSize() : null,
                    variantRepository.countByProductId(productId));
        }
        Map<UUID, ProductImage> images = primaryImages(page.getContent());
        return page.map(variant -> {
            ProductVariantDto dto = variantMapper.toDto(variant);
            ProductImage image = images.get(variant.getId());
            if (image != null) dto.setImageUrl(image.getImageUrl());
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public Page<ProductVariantDto> getAll(UUID productId, String keyword, String color,
            String variantSize, Boolean active, Pageable pageable) {
        validateSort(pageable);
        Page<ProductVariant> page = variantRepository.searchAll(productId, trimToEmpty(keyword),
                trimToEmpty(color), trimToEmpty(variantSize), active, pageable);
        if (page.isEmpty()) {
            log.warn("Variant catalog empty: productId={}, keyword='{}', color='{}', variantSize='{}', active={}, page={}, size={}, variantsInDatabase={}, variantsForProduct={}",
                    productId, trimToEmpty(keyword), trimToEmpty(color), trimToEmpty(variantSize), active,
                    pageable.isPaged() ? pageable.getPageNumber() : null,
                    pageable.isPaged() ? pageable.getPageSize() : null,
                    variantRepository.count(),
                    productId == null ? null : variantRepository.countByProductId(productId));
        }
        Map<UUID, com.fashionsystem.fashion_system.entity.Product> products = productRepository
                .findAllById(page.getContent().stream().map(ProductVariant::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(com.fashionsystem.fashion_system.entity.Product::getId, Function.identity()));
        Map<UUID, ProductImage> images = primaryImages(page.getContent());
        return page.map(variant -> {
            ProductVariantDto dto = variantMapper.toDto(variant);
            var product = products.get(variant.getProductId());
            if (product != null) {
                dto.setProductName(product.getName());
                dto.setProductCode(product.getCode());
            }
            var image = images.get(variant.getId());
            if (image != null) dto.setImageUrl(image.getImageUrl());
            return dto;
        });
    }

    private Map<UUID, ProductImage> primaryImages(List<ProductVariant> variants) {
        Map<UUID, ProductImage> images = new java.util.HashMap<>();
        if (variants.isEmpty()) return images;
        imageRepository.findAllByProductVariantIdIn(variants.stream().map(ProductVariant::getId).toList())
                .forEach(image -> images.merge(image.getProductVariantId(), image,
                        (current, candidate) -> Boolean.TRUE.equals(candidate.getIsPrimary()) ? candidate : current));
        return images;
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
        ensureCombinationAvailable(productId, request, variantId);
        variantMapper.updateEntity(request, entity);
        try {
            return variantMapper.toDto(variantRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("SKU hoặc tổ hợp biến thể đã tồn tại");
        }
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
        entity.setActive(false);
        entity.setUpdatedAt(LocalDateTime.now());
        variantRepository.save(entity);
    }

    /**
     * Kích hoạt một biến thể sản phẩm.
     */
    @Transactional
    @CachePut(cacheNames = CacheNames.PRODUCT_VARIANT_DETAIL,
            key = "#productId + ':' + #variantId")
    public ProductVariantDto activate(UUID productId, UUID variantId) {
        requireActiveProduct(productId);
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

    private void requireActiveProduct(UUID productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound("Sản phẩm không tồn tại"));
        if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
            throw BusinessException.conflict("Không thể kích hoạt biến thể vì sản phẩm không hoạt động");
        }
    }

    private void ensureCombinationAvailable(UUID productId, ProductVariantDto request, UUID excludedId) {
        String color = normalizeCombinationPart(request.getColor());
        String size = normalizeCombinationPart(request.getSize());
        if (variantRepository.existsCombination(productId, color, size, excludedId)) {
            throw BusinessException.conflict("Biến thể màu sắc và kích thước này đã tồn tại");
        }
    }

    private String normalizeCombinationPart(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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
