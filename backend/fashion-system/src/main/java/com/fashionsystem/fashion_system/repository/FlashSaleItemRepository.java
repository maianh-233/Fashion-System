package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.FlashSaleItem;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FlashSaleItemRepository extends BaseRepository<FlashSaleItem, UUID> {
    List<FlashSaleItem> findAllByFlashSaleIdIn(Collection<UUID> flashSaleIds);

    void deleteAllByFlashSaleId(UUID flashSaleId);
}
