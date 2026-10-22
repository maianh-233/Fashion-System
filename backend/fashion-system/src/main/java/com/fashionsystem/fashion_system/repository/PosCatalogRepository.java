package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.dto.order.PosProductRow;
import com.fashionsystem.fashion_system.dto.order.PosVariantRow;
import com.fashionsystem.fashion_system.entity.ProductVariant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Truy vấn chỉ đọc cho màn hình bán tại quầy: biến thể kèm tồn kho offline của một cửa hàng. */
public interface PosCatalogRepository extends Repository<ProductVariant, UUID> {
    String VARIANT_ROW = """
            select new com.fashionsystem.fashion_system.dto.order.PosVariantRow(
                v.id, p.id, p.name, p.code, p.imageUrl, v.sku, v.barcode, v.color, v.size, v.price, v.salePrice,
                v.active, p.status, coalesce(b.availableQuantity, 0))
            from ProductVariant v
            join Product p on p.id = v.productId
            left join InventoryBalance b on b.productVariantId = v.id and b.storeId = :storeId
            """;

    /** Tìm theo SKU hoặc mã vạch (không phân biệt hoa thường); máy quét dạng bàn phím gửi đúng mã này. */
    @Query(VARIANT_ROW + " where upper(v.sku) = :code or upper(v.barcode) = :code")
    List<PosVariantRow> findByCode(@Param("storeId") UUID storeId, @Param("code") String code);

    /** Thông tin bán của biến thể không gắn cửa hàng (đơn online); stock luôn là 0. */
    @Query("""
            select new com.fashionsystem.fashion_system.dto.order.PosVariantRow(
                v.id, p.id, p.name, p.code, p.imageUrl, v.sku, v.barcode, v.color, v.size, v.price, v.salePrice,
                v.active, p.status, 0)
            from ProductVariant v join Product p on p.id = v.productId
            where v.id in :ids
            """)
    List<PosVariantRow> findCatalogByIds(@Param("ids") Collection<UUID> ids);

    @Query(VARIANT_ROW + " where v.id in :ids")
    List<PosVariantRow> findByIds(@Param("storeId") UUID storeId, @Param("ids") Collection<UUID> ids);

    @Query(VARIANT_ROW + """
             where p.id = :productId and v.active = true and p.status = 'ACTIVE' and b.availableQuantity > 0
             order by v.color, v.size, v.sku
            """)
    List<PosVariantRow> findSellableByProduct(@Param("storeId") UUID storeId, @Param("productId") UUID productId);

    @Query(value = """
            select new com.fashionsystem.fashion_system.dto.order.PosProductRow(
                p.id, p.name, p.code, p.imageUrl,
                min(coalesce(v.salePrice, v.price)), max(coalesce(v.salePrice, v.price)),
                sum(b.availableQuantity), count(v.id))
            from Product p
            join ProductVariant v on v.productId = p.id
            join InventoryBalance b on b.productVariantId = v.id and b.storeId = :storeId
            where p.status = 'ACTIVE' and v.active = true and b.availableQuantity > 0
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%'))
                   or lower(p.code) like lower(concat('%', :keyword, '%')))
            group by p.id, p.name, p.code, p.imageUrl
            order by p.name
            """,
            countQuery = """
            select count(distinct p.id)
            from Product p
            join ProductVariant v on v.productId = p.id
            join InventoryBalance b on b.productVariantId = v.id and b.storeId = :storeId
            where p.status = 'ACTIVE' and v.active = true and b.availableQuantity > 0
              and (:keyword = '' or lower(p.name) like lower(concat('%', :keyword, '%'))
                   or lower(p.code) like lower(concat('%', :keyword, '%')))
            """)
    Page<PosProductRow> searchSellableProducts(
            @Param("storeId") UUID storeId, @Param("keyword") String keyword, Pageable pageable);
}
