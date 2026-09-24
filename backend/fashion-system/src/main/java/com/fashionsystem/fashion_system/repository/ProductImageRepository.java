package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ProductImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho ProductImage.
 */
public interface ProductImageRepository extends BaseRepository<ProductImage, UUID> {
    Optional<ProductImage> findByIdAndProductVariantId(UUID id, UUID productVariantId);

    List<ProductImage> findAllByProductVariantIdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
            UUID productVariantId);

    List<ProductImage> findAllByProductVariantIdIn(List<UUID> variantIds);

    @Query("select i from ProductImage i where i.productVariantId = :variantId and i.isPrimary = true")
    List<ProductImage> findPrimaryRows(@Param("variantId") UUID variantId);

    default int clearPrimary(UUID variantId) {
        var rows = findPrimaryRows(variantId);
        rows.forEach(row -> row.setIsPrimary(false));
        saveAllAndFlush(rows);
        return rows.size();
    }
}
