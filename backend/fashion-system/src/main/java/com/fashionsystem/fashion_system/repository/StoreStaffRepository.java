package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.StoreStaff;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho StoreStaff.
 */
public interface StoreStaffRepository extends BaseRepository<StoreStaff, UUID> {
    Optional<StoreStaff> findByIdAndStoreId(UUID id, UUID storeId);
    boolean existsByUserIdAndStoreIdAndActiveTrue(UUID userId, UUID storeId);
    boolean existsByUserIdAndStoreIdAndActiveTrueAndIdNot(UUID userId, UUID storeId, UUID id);

    @Query("""
            select s from StoreStaff s
            where (:storeId is null or s.storeId = :storeId)
              and (:userId is null or s.userId = :userId)
              and (:staffRole = '' or upper(coalesce(s.staffRole, '')) = :staffRole)
              and (:active is null or s.active = :active)
            """)
    Page<StoreStaff> search(
            @Param("storeId") UUID storeId,
            @Param("userId") UUID userId,
            @Param("staffRole") String staffRole,
            @Param("active") Boolean active,
            Pageable pageable);
}
