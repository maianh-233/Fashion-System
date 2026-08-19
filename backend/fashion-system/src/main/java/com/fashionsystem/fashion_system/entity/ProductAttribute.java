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
 * Entity đại diện cho bảng {@code product_attributes}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "product_attributes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAttribute {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã sản phẩm của bản ghi. */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Lưu giá trị attribute name của bản ghi. */
    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;

    /** Lưu giá trị attribute value của bản ghi. */
    @Column(name = "attribute_value", nullable = false, length = 255)
    private String attributeValue;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

}

