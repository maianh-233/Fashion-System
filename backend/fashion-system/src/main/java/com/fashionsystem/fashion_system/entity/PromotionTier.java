package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code promotion_tiers}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "promotion_tiers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(PromotionTierId.class)
public class PromotionTier {
    /** Lưu mã khuyến mãi của bản ghi. */
    @Id
    @Column(name = "promotion_id", nullable = false)
    private UUID promotionId;

    /** Lưu mã hạng khách hàng của bản ghi. */
    @Id
    @Column(name = "tier_id", nullable = false)
    private UUID tierId;

}

