package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

/** Truy cập khách hàng độc lập với UserRepository của nhân viên. */
public interface CustomerRepository extends BaseRepository<Customer, UUID> {
    Optional<Customer> findByUsername(String username);
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsernameAndIdNot(String username, UUID id);
    boolean existsByEmailAndIdNot(String email, UUID id);
    boolean existsByPhoneAndIdNot(String phone, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id = :id")
    Optional<Customer> findByIdForUpdate(@Param("id") UUID id);

    @Query("select c from Customer c where c.username = :username or c.email = :email")
    List<Customer> findRegistrationConflicts(
            @Param("username") String username, @Param("email") String email);

    @Query("""
            select c from Customer c
            where (:keyword = ''
                or lower(c.username) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(c.email, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(c.phone, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(c.fullName, '')) like lower(concat('%', :keyword, '%')))
              and (:active is null or c.active = :active)
              and (:locked is null or c.locked = :locked)
              and (:gender = '' or c.gender = :gender)
              and (:createdFrom is null or c.createdAt >= :createdFrom)
              and (:createdTo is null or c.createdAt <= :createdTo)
            """)
    Page<Customer> search(
            @Param("keyword") String keyword,
            @Param("active") Boolean active,
            @Param("locked") Boolean locked,
            @Param("gender") String gender,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable);

    @Query("""
            select c from Customer c
            where exists (
                select a.id from CustomerTierAssignment a
                where a.customerId = c.id and a.tierId = :tierId and a.expiresAt is null)
            """)
    Page<Customer> findCustomersByCurrentTier(@Param("tierId") UUID tierId, Pageable pageable);
}
