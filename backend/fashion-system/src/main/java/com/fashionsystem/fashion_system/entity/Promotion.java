package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code promotions}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "promotions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã nghiệp vụ của bản ghi. */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** Lưu tên hiển thị của bản ghi. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Lưu giá trị discount type của bản ghi. */
    @Column(name = "discount_type", nullable = false, columnDefinition = "discount_type_enum")
    private String discountType;

    /** Lưu giá trị discount value của bản ghi. */
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /** Lưu giá trị start date của bản ghi. */
    @Column(name = "start_date")
    private LocalDateTime startDate;

    /** Lưu giá trị end date của bản ghi. */
    @Column(name = "end_date")
    private LocalDateTime endDate;

    /** Lưu giá trị min order value của bản ghi. */
    @Column(name = "min_order_value", precision = 14, scale = 2)
    private BigDecimal minOrderValue;

    /** Lưu giá trị max discount của bản ghi. */
    @Column(name = "max_discount", precision = 14, scale = 2)
    private BigDecimal maxDiscount;

    /** Lưu giá trị usage limit của bản ghi. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /** Lưu giá trị usage per user của bản ghi. */
    @Column(name = "usage_per_user")
    private Integer usagePerUser;

    /** Lưu trạng thái kích hoạt của bản ghi. */
    @Column(name = "active")
    private Boolean active;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

