package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.ReturnRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRequestRepository extends BaseRepository<ReturnRequest, UUID> {
    List<ReturnRequest> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    List<ReturnRequest> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReturnRequest r where r.id = :id")
    Optional<ReturnRequest> findByIdForUpdate(@Param("id") UUID id);
}
