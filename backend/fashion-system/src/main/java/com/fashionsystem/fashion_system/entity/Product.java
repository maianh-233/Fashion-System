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
 * Entity đại diện cho bảng {@code products}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã thương hiệu của bản ghi. */
    @Column(name = "brand_id")
    private UUID brandId;

    /** Lưu mã bộ sưu tập của bản ghi. */
    @Column(name = "collection_id")
    private UUID collectionId;

    /** Lưu mã danh mục của bản ghi. */
    @Column(name = "category_id")
    private UUID categoryId;

    /** Lưu tên hiển thị của bản ghi. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Lưu giá trị slug của bản ghi. */
    @Column(name = "slug", unique = true, length = 255)
    private String slug;

    /** Lưu mô tả chi tiết của bản ghi. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Lưu giá trị material của bản ghi. */
    @Column(name = "material", length = 255)
    private String material;

    /** Lưu giá trị fit của bản ghi. */
    @Column(name = "fit", length = 100)
    private String fit;

    /** Lưu giá trị gender của bản ghi. */
    @Column(name = "gender", length = 20)
    private String gender;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", length = 50)
    private String status;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Lưu giá trị image url của bản ghi. */
    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

}

