package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ảnh khách đính kèm đánh giá (bảng {@code product_review_images}), tối đa 3 ảnh mỗi đánh giá. */
@Entity
@Table(name = "product_review_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewImage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    /** Public id trên Cloudinary, dùng để xóa ảnh. */
    @Column(name = "public_id")
    private String publicId;

    /** 0..2. */
    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;
}
