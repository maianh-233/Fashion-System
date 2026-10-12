package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.LoyaltyAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho LoyaltyAccount.
 */
public interface LoyaltyAccountRepository extends BaseRepository<LoyaltyAccount, UUID> {
    /** Khóa ví điểm để hai lần đổi đồng thời không trừ vượt số dư. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoyaltyAccount a where a.customerId = :customerId")
    Optional<LoyaltyAccount> findByCustomerIdForUpdate(@Param("customerId") UUID customerId);
}
