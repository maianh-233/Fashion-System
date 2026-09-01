package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.GoodsReceipt;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho GoodsReceipt.
 */
public interface GoodsReceiptRepository extends BaseRepository<GoodsReceipt, UUID> {
    boolean existsByReceiptCode(String receiptCode);
    boolean existsByReceiptCodeAndIdNot(String receiptCode, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from GoodsReceipt r where r.id = :id")
    Optional<GoodsReceipt> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select r from GoodsReceipt r
            where (:keyword = '' or lower(r.receiptCode) like lower(concat('%', :keyword, '%')))
              and (:storeId is null or r.storeId = :storeId)
              and (:supplierId is null or r.supplierId = :supplierId)
              and (:status = '' or upper(r.status) = :status)
              and (:fromDate is null or r.receiptDate >= :fromDate)
              and (:toDate is null or r.receiptDate <= :toDate)
            """)
    Page<GoodsReceipt> search(
            @Param("keyword") String keyword, @Param("storeId") UUID storeId,
            @Param("supplierId") UUID supplierId, @Param("status") String status,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate, Pageable pageable);
}
