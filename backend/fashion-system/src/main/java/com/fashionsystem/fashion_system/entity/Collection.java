package com.fashionsystem.fashion_system.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity đại diện cho bảng {@code collections}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "collections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Collection {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã thương hiệu của bản ghi. */
    @Column(name = "brand_id")
    private UUID brandId;

    /** Lưu tên hiển thị của bản ghi. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Lưu mã nghiệp vụ của bản ghi. */
    @Column(name = "code", unique = true, length = 100)
    private String code;

    /** Lưu giá trị season của bản ghi. */
    @Column(name = "season", length = 50)
    private String season;

    /** Lưu giá trị year của bản ghi. */
    @Column(name = "year")
    private Integer year;

    /** Lưu giá trị release date của bản ghi. */
    @Column(name = "release_date")
    private LocalDate releaseDate;

    /** Lưu mô tả chi tiết của bản ghi. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Lưu giá trị image url của bản ghi. */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", length = 50)
    private String status;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

