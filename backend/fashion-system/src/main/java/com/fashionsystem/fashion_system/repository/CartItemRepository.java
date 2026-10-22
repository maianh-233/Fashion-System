package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CartItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends BaseRepository<CartItem, UUID> {
    List<CartItem> findAllByCustomerIdOrderByCreatedAtAsc(UUID customerId);

    Optional<CartItem> findByCustomerIdAndProductVariantId(UUID customerId, UUID productVariantId);

    long countByCustomerId(UUID customerId);

    /** Bỏ các biến thể vừa đặt hàng khỏi giỏ. */
    @Modifying
    @Query("delete from CartItem c where c.customerId = :customerId and c.productVariantId in :variantIds")
    int deleteVariants(@Param("customerId") UUID customerId, @Param("variantIds") Collection<UUID> variantIds);
}
