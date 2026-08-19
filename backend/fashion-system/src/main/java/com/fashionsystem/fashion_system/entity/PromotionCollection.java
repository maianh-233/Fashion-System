package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code promotion_collections}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "promotion_collections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(PromotionCollectionId.class)
public class PromotionCollection {
    /** Lưu mã khuyến mãi của bản ghi. */
    @Id
    @Column(name = "promotion_id", nullable = false)
    private UUID promotionId;

    /** Lưu mã bộ sưu tập của bản ghi. */
    @Id
    @Column(name = "collection_id", nullable = false)
    private UUID collectionId;

}

