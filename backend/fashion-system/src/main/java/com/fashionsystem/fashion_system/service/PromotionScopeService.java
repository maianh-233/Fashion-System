package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.PromotionBrandDto;
import com.fashionsystem.fashion_system.dto.PromotionCategoryDto;
import com.fashionsystem.fashion_system.dto.PromotionCollectionDto;
import com.fashionsystem.fashion_system.dto.PromotionProductDto;
import com.fashionsystem.fashion_system.dto.PromotionTierDto;
import com.fashionsystem.fashion_system.entity.PromotionBrand;
import com.fashionsystem.fashion_system.entity.PromotionCategory;
import com.fashionsystem.fashion_system.entity.PromotionCollection;
import com.fashionsystem.fashion_system.entity.PromotionProduct;
import com.fashionsystem.fashion_system.entity.PromotionTier;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.mapper.PromotionBrandMapper;
import com.fashionsystem.fashion_system.mapper.PromotionCategoryMapper;
import com.fashionsystem.fashion_system.mapper.PromotionCollectionMapper;
import com.fashionsystem.fashion_system.mapper.PromotionProductMapper;
import com.fashionsystem.fashion_system.mapper.PromotionTierMapper;
import com.fashionsystem.fashion_system.repository.BrandRepository;
import com.fashionsystem.fashion_system.repository.CategoryRepository;
import com.fashionsystem.fashion_system.repository.CollectionRepository;
import com.fashionsystem.fashion_system.repository.CustomerTierRepository;
import com.fashionsystem.fashion_system.repository.ProductRepository;
import com.fashionsystem.fashion_system.repository.PromotionBrandRepository;
import com.fashionsystem.fashion_system.repository.PromotionCategoryRepository;
import com.fashionsystem.fashion_system.repository.PromotionCollectionRepository;
import com.fashionsystem.fashion_system.repository.PromotionProductRepository;
import com.fashionsystem.fashion_system.repository.PromotionRepository;
import com.fashionsystem.fashion_system.repository.PromotionTierRepository;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quản lý phạm vi brand, category, collection, product và tier của khuyến mãi. */
@Service
@com.fashionsystem.fashion_system.audit.BusinessAudit("PROMOTION")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PromotionScopeService {
    private final PromotionRepository promotionRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;
    private final ProductRepository productRepository;
    private final CustomerTierRepository tierRepository;
    private final PromotionBrandRepository promotionBrandRepository;
    private final PromotionCategoryRepository promotionCategoryRepository;
    private final PromotionCollectionRepository promotionCollectionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final PromotionTierRepository promotionTierRepository;
    private final PromotionBrandMapper promotionBrandMapper;
    private final PromotionCategoryMapper promotionCategoryMapper;
    private final PromotionCollectionMapper promotionCollectionMapper;
    private final PromotionProductMapper promotionProductMapper;
    private final PromotionTierMapper promotionTierMapper;

    /** Gắn thương hiệu vào phạm vi khuyến mãi. */
    @Transactional
    public PromotionBrandDto addBrand(UUID promotionId, UUID brandId) {
        requirePromotion(promotionId);
        if (!brandRepository.existsById(brandId)) throw BusinessException.notFound("Thương hiệu không tồn tại");
        if (promotionBrandRepository.existsByPromotionIdAndBrandId(promotionId, brandId))
            throw BusinessException.conflict("Thương hiệu đã thuộc phạm vi khuyến mãi");
        return promotionBrandMapper.toDto(promotionBrandRepository.save(
                PromotionBrand.builder().promotionId(promotionId).brandId(brandId).build()));
    }

    /** Gỡ thương hiệu khỏi phạm vi khuyến mãi. */
    @Transactional
    public void removeBrand(UUID promotionId, UUID brandId) {
        requireMapping(promotionBrandRepository.existsByPromotionIdAndBrandId(promotionId, brandId));
        promotionBrandRepository.deleteByPromotionIdAndBrandId(promotionId, brandId);
    }

    /** Lấy các thương hiệu thuộc phạm vi khuyến mãi. */
    @Transactional(readOnly = true)
    public Page<PromotionBrandDto> getBrands(UUID promotionId, Pageable pageable) {
        requirePromotion(promotionId); validateSort(pageable, "brandId");
        return promotionBrandRepository.findAllByPromotionId(promotionId, pageable).map(promotionBrandMapper::toDto);
    }

    /** Gắn danh mục vào phạm vi khuyến mãi. */
    @Transactional
    public PromotionCategoryDto addCategory(UUID promotionId, UUID categoryId) {
        requirePromotion(promotionId);
        if (!categoryRepository.existsById(categoryId)) throw BusinessException.notFound("Danh mục không tồn tại");
        if (promotionCategoryRepository.existsByPromotionIdAndCategoryId(promotionId, categoryId))
            throw BusinessException.conflict("Danh mục đã thuộc phạm vi khuyến mãi");
        return promotionCategoryMapper.toDto(promotionCategoryRepository.save(
                PromotionCategory.builder().promotionId(promotionId).categoryId(categoryId).build()));
    }

    /** Gỡ danh mục khỏi phạm vi khuyến mãi. */
    @Transactional
    public void removeCategory(UUID promotionId, UUID categoryId) {
        requireMapping(promotionCategoryRepository.existsByPromotionIdAndCategoryId(promotionId, categoryId));
        promotionCategoryRepository.deleteByPromotionIdAndCategoryId(promotionId, categoryId);
    }

    /** Lấy các danh mục thuộc phạm vi khuyến mãi. */
    @Transactional(readOnly = true)
    public Page<PromotionCategoryDto> getCategories(UUID promotionId, Pageable pageable) {
        requirePromotion(promotionId); validateSort(pageable, "categoryId");
        return promotionCategoryRepository.findAllByPromotionId(promotionId, pageable).map(promotionCategoryMapper::toDto);
    }

    /** Gắn bộ sưu tập vào phạm vi khuyến mãi. */
    @Transactional
    public PromotionCollectionDto addCollection(UUID promotionId, UUID collectionId) {
        requirePromotion(promotionId);
        if (!collectionRepository.existsById(collectionId)) throw BusinessException.notFound("Bộ sưu tập không tồn tại");
        if (promotionCollectionRepository.existsByPromotionIdAndCollectionId(promotionId, collectionId))
            throw BusinessException.conflict("Bộ sưu tập đã thuộc phạm vi khuyến mãi");
        return promotionCollectionMapper.toDto(promotionCollectionRepository.save(
                PromotionCollection.builder().promotionId(promotionId).collectionId(collectionId).build()));
    }

    /** Gỡ bộ sưu tập khỏi phạm vi khuyến mãi. */
    @Transactional
    public void removeCollection(UUID promotionId, UUID collectionId) {
        requireMapping(promotionCollectionRepository.existsByPromotionIdAndCollectionId(promotionId, collectionId));
        promotionCollectionRepository.deleteByPromotionIdAndCollectionId(promotionId, collectionId);
    }

    /** Lấy các bộ sưu tập thuộc phạm vi khuyến mãi. */
    @Transactional(readOnly = true)
    public Page<PromotionCollectionDto> getCollections(UUID promotionId, Pageable pageable) {
        requirePromotion(promotionId); validateSort(pageable, "collectionId");
        return promotionCollectionRepository.findAllByPromotionId(promotionId, pageable).map(promotionCollectionMapper::toDto);
    }

    /** Gắn sản phẩm vào phạm vi khuyến mãi. */
    @Transactional
    public PromotionProductDto addProduct(UUID promotionId, UUID productId) {
        requirePromotion(promotionId);
        if (!productRepository.existsById(productId)) throw BusinessException.notFound("Sản phẩm không tồn tại");
        if (promotionProductRepository.existsByPromotionIdAndProductId(promotionId, productId))
            throw BusinessException.conflict("Sản phẩm đã thuộc phạm vi khuyến mãi");
        return promotionProductMapper.toDto(promotionProductRepository.save(
                PromotionProduct.builder().promotionId(promotionId).productId(productId).build()));
    }

    /** Gỡ sản phẩm khỏi phạm vi khuyến mãi. */
    @Transactional
    public void removeProduct(UUID promotionId, UUID productId) {
        requireMapping(promotionProductRepository.existsByPromotionIdAndProductId(promotionId, productId));
        promotionProductRepository.deleteByPromotionIdAndProductId(promotionId, productId);
    }

    /** Lấy các sản phẩm thuộc phạm vi khuyến mãi. */
    @Transactional(readOnly = true)
    public Page<PromotionProductDto> getProducts(UUID promotionId, Pageable pageable) {
        requirePromotion(promotionId); validateSort(pageable, "productId");
        return promotionProductRepository.findAllByPromotionId(promotionId, pageable).map(promotionProductMapper::toDto);
    }

    /** Gắn hạng khách hàng vào phạm vi khuyến mãi. */
    @Transactional
    public PromotionTierDto addTier(UUID promotionId, UUID tierId) {
        requirePromotion(promotionId);
        if (!tierRepository.existsById(tierId)) throw BusinessException.notFound("Hạng khách hàng không tồn tại");
        if (promotionTierRepository.existsByPromotionIdAndTierId(promotionId, tierId))
            throw BusinessException.conflict("Hạng khách hàng đã thuộc phạm vi khuyến mãi");
        return promotionTierMapper.toDto(promotionTierRepository.save(
                PromotionTier.builder().promotionId(promotionId).tierId(tierId).build()));
    }

    /** Gỡ hạng khách hàng khỏi phạm vi khuyến mãi. */
    @Transactional
    public void removeTier(UUID promotionId, UUID tierId) {
        requireMapping(promotionTierRepository.existsByPromotionIdAndTierId(promotionId, tierId));
        promotionTierRepository.deleteByPromotionIdAndTierId(promotionId, tierId);
    }

    /** Lấy các hạng khách hàng thuộc phạm vi khuyến mãi. */
    @Transactional(readOnly = true)
    public Page<PromotionTierDto> getTiers(UUID promotionId, Pageable pageable) {
        requirePromotion(promotionId); validateSort(pageable, "tierId");
        return promotionTierRepository.findAllByPromotionId(promotionId, pageable).map(promotionTierMapper::toDto);
    }

    private void requirePromotion(UUID promotionId) {
        if (!promotionRepository.existsById(promotionId)) throw BusinessException.notFound("Khuyến mãi không tồn tại");
    }
    private void requireMapping(boolean exists) {
        if (!exists) throw BusinessException.notFound("Phạm vi khuyến mãi không tồn tại");
    }
    private void validateSort(Pageable pageable, String targetField) {
        Set<String> fields = Set.of("promotionId", targetField);
        if (pageable.getSort().stream().anyMatch(o -> !fields.contains(o.getProperty())))
            throw BusinessException.badRequest("Trường sắp xếp phạm vi khuyến mãi không hợp lệ");
    }
}
