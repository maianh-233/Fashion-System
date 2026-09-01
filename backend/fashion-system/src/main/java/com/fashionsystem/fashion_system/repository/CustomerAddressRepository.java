package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerAddress;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho CustomerAddress.
 */
public interface CustomerAddressRepository extends BaseRepository<CustomerAddress, UUID> {
    Page<CustomerAddress> findAllByCustomerId(UUID customerId, Pageable pageable);

    Optional<CustomerAddress> findByIdAndCustomerId(UUID id, UUID customerId);

    Optional<CustomerAddress> findByCustomerIdAndIsDefaultTrue(UUID customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CustomerAddress a set a.isDefault = false, a.updatedAt = :updatedAt
            where a.customerId = :customerId and a.isDefault = true
            """)
    int clearDefault(
            @Param("customerId") UUID customerId,
            @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
