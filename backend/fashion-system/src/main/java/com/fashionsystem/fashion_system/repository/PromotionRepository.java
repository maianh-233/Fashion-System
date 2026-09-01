package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Promotion;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Promotion.
 */
public interface PromotionRepository extends BaseRepository<Promotion, UUID> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);
    Optional<Promotion> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select p from Promotion p
            where (:keyword = ''
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.code) like lower(concat('%', :keyword, '%')))
              and (:discountType = '' or upper(p.discountType) = :discountType)
              and (:active is null or p.active = :active)
              and (:effectiveAt is null or (p.startDate is null or p.startDate <= :effectiveAt))
              and (:effectiveAt is null or (p.endDate is null or p.endDate >= :effectiveAt))
            """)
    Page<Promotion> search(
            @Param("keyword") String keyword, @Param("discountType") String discountType,
            @Param("active") Boolean active, @Param("effectiveAt") java.time.LocalDateTime effectiveAt,
            Pageable pageable);
}
