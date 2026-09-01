package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.GoodsIssue;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho GoodsIssue.
 */
public interface GoodsIssueRepository extends BaseRepository<GoodsIssue, UUID> {
    boolean existsByIssueCode(String issueCode);
    boolean existsByIssueCodeAndIdNot(String issueCode, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from GoodsIssue i where i.id = :id")
    Optional<GoodsIssue> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select i from GoodsIssue i
            where (:keyword = '' or lower(i.issueCode) like lower(concat('%', :keyword, '%')))
              and (:storeId is null or i.storeId = :storeId)
              and (:orderId is null or i.orderId = :orderId)
              and (:issueType = '' or upper(i.issueType) = :issueType)
              and (:status = '' or upper(i.status) = :status)
              and (:fromDate is null or i.issueDate >= :fromDate)
              and (:toDate is null or i.issueDate <= :toDate)
            """)
    Page<GoodsIssue> search(
            @Param("keyword") String keyword, @Param("storeId") UUID storeId,
            @Param("orderId") UUID orderId, @Param("issueType") String issueType,
            @Param("status") String status, @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate, Pageable pageable);
}
