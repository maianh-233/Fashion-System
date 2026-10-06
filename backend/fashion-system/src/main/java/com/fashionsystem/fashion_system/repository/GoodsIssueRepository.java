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

    default Page<GoodsIssue> search(String keyword, UUID storeId, UUID orderId, String issueType,
            String status, java.time.LocalDateTime fromDate, java.time.LocalDateTime toDate, Pageable pageable) {
        return searchFiltered(keyword, storeId, orderId != null, orderId, issueType, status,
                fromDate != null, fromDate, toDate != null, toDate, pageable);
    }

    @Query("""
            select i from GoodsIssue i
            where (:keyword = '' or lower(i.issueCode) like lower(concat('%', :keyword, '%')))
              and i.storeId = :storeId
              and (:filterOrder = false or i.orderId = :orderId)
              and (:issueType = '' or upper(i.issueType) = :issueType)
              and (:status = '' or upper(i.status) = :status)
              and (:filterFrom = false or i.issueDate >= :fromDate)
              and (:filterTo = false or i.issueDate < :toDate)
            """)
    Page<GoodsIssue> searchFiltered(
            @Param("keyword") String keyword, @Param("storeId") UUID storeId,
            @Param("filterOrder") boolean filterOrder, @Param("orderId") UUID orderId,
            @Param("issueType") String issueType, @Param("status") String status,
            @Param("filterFrom") boolean filterFrom, @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("filterTo") boolean filterTo, @Param("toDate") java.time.LocalDateTime toDate,
            Pageable pageable);
}
