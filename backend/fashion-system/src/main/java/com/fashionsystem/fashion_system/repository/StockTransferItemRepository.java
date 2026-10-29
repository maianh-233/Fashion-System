package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.StockTransferItem;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StockTransferItemRepository extends BaseRepository<StockTransferItem, UUID> {
    List<StockTransferItem> findAllByTransferIdIn(Collection<UUID> transferIds);
}
