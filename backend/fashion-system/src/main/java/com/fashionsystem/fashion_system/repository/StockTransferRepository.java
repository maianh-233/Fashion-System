package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.StockTransfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockTransferRepository extends BaseRepository<StockTransfer, UUID> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from StockTransfer t where t.id = :id")
    Optional<StockTransfer> findByIdForUpdate(@Param("id") UUID id);
}
