package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.GoodsReceiptItem;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho GoodsReceiptItem.
 */
public interface GoodsReceiptItemRepository extends BaseRepository<GoodsReceiptItem, UUID> {
    Optional<GoodsReceiptItem> findByIdAndReceiptId(UUID id, UUID receiptId);
    List<GoodsReceiptItem> findAllByReceiptIdOrderByCreatedAtAsc(UUID receiptId);

    @Query("select coalesce(sum(i.quantity), 0) from GoodsReceiptItem i where i.receiptId = :receiptId")
    Integer sumQuantity(@Param("receiptId") UUID receiptId);

    @Query("select coalesce(sum(i.total), 0) from GoodsReceiptItem i where i.receiptId = :receiptId")
    BigDecimal sumTotal(@Param("receiptId") UUID receiptId);
}
