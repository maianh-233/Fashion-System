package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.dto.customer.CustomerListResponse;
import com.fashionsystem.fashion_system.entity.Customer;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends BaseRepository<Customer, UUID> {
    Optional<Customer> findByNormalizedPhone(String normalizedPhone);
    Optional<Customer> findByEmailIgnoreCase(String email);
    Optional<Customer> findByCustomerCodeIgnoreCase(String customerCode);
    boolean existsByCustomerCode(String customerCode);
    boolean existsByNormalizedPhoneAndIdNot(String normalizedPhone, UUID id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Customer c where c.id=:id")
    Optional<Customer> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select new com.fashionsystem.fashion_system.dto.customer.CustomerListResponse(
                c.id, c.customerCode, c.fullName, c.phone, c.email, c.source,
                c.membershipStatus, c.originStoreId, s.name, t.code,
                case when a.id is null then false else true end, c.active, c.createdAt)
            from Customer c
            left join Store s on s.id=c.originStoreId
            left join CustomerAccount a on a.customerId=c.id
            left join CustomerTierAssignment ta on ta.customerId=c.id and ta.expiresAt is null
            left join CustomerTier t on t.id=ta.tierId
            where (:keyword='' or lower(c.customerCode) like lower(concat('%',:keyword,'%'))
                or lower(coalesce(c.fullName,'')) like lower(concat('%',:keyword,'%'))
                or lower(coalesce(c.phone,'')) like lower(concat('%',:keyword,'%'))
                or lower(coalesce(c.email,'')) like lower(concat('%',:keyword,'%')))
              and (:storeId is null or c.originStoreId=:storeId)
              and (:noStore=false or c.originStoreId is null)
              and (:source is null or c.source=:source)
              and (:tierCode='' or t.code=:tierCode)
              and (:hasWebAccount is null
                   or (:hasWebAccount=true and a.id is not null)
                   or (:hasWebAccount=false and a.id is null))
              and (:active is null or c.active=:active)
            """)
    Page<CustomerListResponse> searchManagement(
            @Param("keyword") String keyword,
            @Param("storeId") UUID storeId,
            @Param("noStore") boolean noStore,
            @Param("source") CustomerSource source,
            @Param("tierCode") String tierCode,
            @Param("hasWebAccount") Boolean hasWebAccount,
            @Param("active") Boolean active,
            Pageable pageable);

    @Query("""
            select c from Customer c
            where lower(c.customerCode)=lower(:value)
               or c.normalizedPhone=:value
               or lower(coalesce(c.email,''))=lower(:value)
            """)
    Optional<Customer> findExactLookup(@Param("value") String value);

    @Query("""
            select c from Customer c where exists (
                select a.id from CustomerTierAssignment a
                where a.customerId=c.id and a.tierId=:tierId and a.expiresAt is null)
            """)
    Page<Customer> findCustomersByCurrentTier(@Param("tierId") UUID tierId, Pageable pageable);
}
