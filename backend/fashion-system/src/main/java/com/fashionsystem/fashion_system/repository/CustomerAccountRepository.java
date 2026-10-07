package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence riêng của tài khoản đăng nhập Customer. */
public interface CustomerAccountRepository extends BaseRepository<CustomerAccount, UUID> {
    Optional<CustomerAccount> findByCustomerId(UUID customerId);

    Optional<CustomerAccount> findByUsername(String username);

    Optional<CustomerAccount> findByLoginEmailIgnoreCase(String loginEmail);

    boolean existsByCustomerId(UUID customerId);

    boolean existsByUsername(String username);

    boolean existsByLoginEmailIgnoreCase(String loginEmail);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CustomerAccount a where a.username = :username")
    Optional<CustomerAccount> findByUsernameForUpdate(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CustomerAccount a where a.customerId = :customerId")
    Optional<CustomerAccount> findByCustomerIdForUpdate(@Param("customerId") UUID customerId);
}

