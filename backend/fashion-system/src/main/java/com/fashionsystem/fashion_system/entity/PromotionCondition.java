package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code promotion_conditions}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "promotion_conditions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCondition {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã khuyến mãi của bản ghi. */
    @Column(name = "promotion_id", nullable = false)
    private UUID promotionId;

    /** Lưu giá trị condition type của bản ghi. */
    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;

    /** Lưu giá trị condition value của bản ghi. */
    @Column(name = "condition_value", nullable = false, length = 255)
    private String conditionValue;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

