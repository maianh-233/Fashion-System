package com.fashionsystem.fashion_system.repository;

import com.fashionsystem.fashion_system.entity.Promotion;
import com.fashionsystem.fashion_system.entity.PromotionStatus;
import com.fashionsystem.fashion_system.entity.PromotionType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Cung cấp các thao tác CRUD cơ bản cho Promotion.
 */
public interface PromotionRepository extends BaseRepository<Promotion, UUID> {
    LocalDateTime SEARCH_FROM = LocalDateTime.of(1970, 1, 1, 0, 0);
    LocalDateTime SEARCH_TO = LocalDateTime.of(9999, 12, 31, 23, 59);

    boolean existsByCode(String code);
    Optional<Promotion> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Tìm kiếm theo từ khóa, loại, trạng thái và khoảng ngày giao với thời gian hiệu lực. Khoảng ngày luôn có giá
     * trị (dùng {@link #SEARCH_FROM}/{@link #SEARCH_TO} khi không lọc) vì PostgreSQL không suy ra kiểu của tham số
     * thời gian null.
     */
    @Query("""
            select p from Promotion p
            where (:keyword = ''
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.code) like lower(concat('%', :keyword, '%')))
              and (:promotionType is null or p.promotionType = :promotionType)
              and (:status is null or p.status = :status)
              and (:includeInactive = true
                or p.status <> com.fashionsystem.fashion_system.entity.PromotionStatus.INACTIVE)
              and p.endDate >= :dateFrom
              and p.startDate <= :dateTo
            """)
    Page<Promotion> search(
            @Param("keyword") String keyword,
            @Param("promotionType") PromotionType promotionType,
            @Param("status") PromotionStatus status,
            @Param("includeInactive") boolean includeInactive,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            Pageable pageable);

    /** Khuyến mãi đổi điểm đang hoạt động mà khách thuộc hạng {@code tierId} được đổi (không gắn hạng = mọi khách). */
    @Query("""
            select p from Promotion p
            where p.promotionType = com.fashionsystem.fashion_system.entity.PromotionType.POINT_REDEMPTION
              and p.status = com.fashionsystem.fashion_system.entity.PromotionStatus.ACTIVE
              and (:keyword = ''
                or lower(p.name) like lower(concat('%', :keyword, '%'))
                or lower(p.code) like lower(concat('%', :keyword, '%')))
              and (not exists (select 1 from PromotionTier t where t.promotionId = p.id)
                or exists (select 1 from PromotionTier t where t.promotionId = p.id and t.tierId = :tierId))
            """)
    Page<Promotion> findRedeemable(
            @Param("keyword") String keyword, @Param("tierId") UUID tierId, Pageable pageable);

    /** Khuyến mãi đang hiệu lực tại thời điểm {@code at} thuộc các loại cho trước. */
    @Query("""
            select p from Promotion p
            where p.status = com.fashionsystem.fashion_system.entity.PromotionStatus.ACTIVE
              and p.startDate <= :at and p.endDate > :at and p.promotionType in :types
            """)
    List<Promotion> findEffective(@Param("at") LocalDateTime at,
                                  @Param("types") Collection<com.fashionsystem.fashion_system.entity.PromotionType> types);

    List<Promotion> findAllByCodeIn(Collection<String> codes);

    /** Khuyến mãi đã tới ngày bắt đầu. */
    List<Promotion> findAllByStatusAndStartDateLessThanEqual(PromotionStatus status, LocalDateTime at);

    /** Khuyến mãi đã quá ngày kết thúc. */
    List<Promotion> findAllByStatusInAndEndDateLessThanEqual(Collection<PromotionStatus> statuses, LocalDateTime at);

    /** Khuyến mãi đang chạy, sắp hết hạn và chưa được nhắc. */
    List<Promotion> findAllByStatusAndEndDateGreaterThanAndEndDateLessThanEqualAndExpiryRemindedAtIsNull(
            PromotionStatus status, LocalDateTime from, LocalDateTime to);
}
