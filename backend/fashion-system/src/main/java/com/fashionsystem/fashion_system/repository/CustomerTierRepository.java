package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.CustomerTier;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho CustomerTier.
 */
public interface CustomerTierRepository extends BaseRepository<CustomerTier, UUID> {
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<CustomerTier> findAllByOrderByMinTotalSpentAscCodeAsc();

    Optional<CustomerTier> findFirstByMinTotalSpentLessThanEqualOrderByMinTotalSpentDesc(BigDecimal totalSpent);

    @Query("""
            select t from CustomerTier t
            where (:keyword = ''
                or lower(t.code) like lower(concat('%', :keyword, '%'))
                or lower(t.name) like lower(concat('%', :keyword, '%')))
              and (:minSpentFrom is null or t.minTotalSpent >= :minSpentFrom)
              and (:minSpentTo is null or t.minTotalSpent <= :minSpentTo)
              and (:discountFrom is null or t.discountPercent >= :discountFrom)
              and (:discountTo is null or t.discountPercent <= :discountTo)
            """)
    Page<CustomerTier> search(
            @Param("keyword") String keyword,
            @Param("minSpentFrom") BigDecimal minSpentFrom,
            @Param("minSpentTo") BigDecimal minSpentTo,
            @Param("discountFrom") BigDecimal discountFrom,
            @Param("discountTo") BigDecimal discountTo,
            Pageable pageable);

    @Query("""
            select t from CustomerTier t join CustomerTierAssignment a on a.tierId = t.id
            where a.customerId = :customerId and a.expiresAt is null
            """)
    Optional<CustomerTier> findCurrentByCustomerId(@Param("customerId") UUID customerId);
}
