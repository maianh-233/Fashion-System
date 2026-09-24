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

    @Query("select a from CustomerAddress a where a.customerId = :customerId and a.isDefault = true")
    java.util.List<CustomerAddress> findDefaults(@Param("customerId") UUID customerId);

    default int clearDefault(UUID customerId, java.time.LocalDateTime updatedAt) {
        var rows = findDefaults(customerId);
        rows.forEach(row -> { row.setIsDefault(false); row.setUpdatedAt(updatedAt); });
        saveAllAndFlush(rows);
        return rows.size();
    }
}
