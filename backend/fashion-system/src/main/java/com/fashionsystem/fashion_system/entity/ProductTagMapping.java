package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code product_tag_mappings}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "product_tag_mappings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(ProductTagMappingId.class)
public class ProductTagMapping {
    /** Lưu mã sản phẩm của bản ghi. */
    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** Lưu mã tham chiếu đến tag. */
    @Id
    @Column(name = "tag_id", nullable = false)
    private UUID tagId;

}

