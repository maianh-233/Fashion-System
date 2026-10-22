package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.LoyaltyTransaction;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Cung cấp các thao tác CRUD cơ bản cho LoyaltyTransaction.
 */
public interface LoyaltyTransactionRepository extends BaseRepository<LoyaltyTransaction, UUID> {
    boolean existsByTransactionTypeAndReferenceTypeAndReferenceId(String transactionType, String referenceType,
                                                                  UUID referenceId);

    List<LoyaltyTransaction> findAllByReferenceTypeAndReferenceIdIn(String referenceType, Collection<UUID> referenceIds);

    Page<LoyaltyTransaction> findAllByCustomerId(UUID customerId, Pageable pageable);
}
