package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerTierAssignment;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerTierAssignmentRepository extends BaseRepository<CustomerTierAssignment, UUID> {
    List<CustomerTierAssignment> findAllByCustomerId(UUID customerId);

    Optional<CustomerTierAssignment> findByCustomerIdAndExpiresAtIsNull(UUID customerId);

    Page<CustomerTierAssignment> findAllByCustomerId(UUID customerId, Pageable pageable);

    boolean existsByTierId(UUID tierId);

    @Query("select a from CustomerTierAssignment a where a.customerId = :customerId and a.expiresAt is null")
    List<CustomerTierAssignment> findCurrentRows(@Param("customerId") UUID customerId);

    default int expireCurrent(UUID customerId, LocalDateTime expiredAt) {
        var rows = findCurrentRows(customerId);
        rows.forEach(row -> row.setExpiresAt(expiredAt));
        saveAllAndFlush(rows);
        return rows.size();
    }

    @Query("""
            select a from CustomerTierAssignment a
            where (:customerId is null or a.customerId = :customerId)
              and (:tierId is null or a.tierId = :tierId)
              and (:assignedFrom is null or a.assignedAt >= :assignedFrom)
              and (:assignedTo is null or a.assignedAt <= :assignedTo)
              and (:expiresFrom is null or a.expiresAt >= :expiresFrom)
              and (:expiresTo is null or a.expiresAt <= :expiresTo)
            """)
    Page<CustomerTierAssignment> search(
            @Param("customerId") UUID customerId,
            @Param("tierId") UUID tierId,
            @Param("assignedFrom") LocalDateTime assignedFrom,
            @Param("assignedTo") LocalDateTime assignedTo,
            @Param("expiresFrom") LocalDateTime expiresFrom,
            @Param("expiresTo") LocalDateTime expiresTo,
            Pageable pageable);
}
