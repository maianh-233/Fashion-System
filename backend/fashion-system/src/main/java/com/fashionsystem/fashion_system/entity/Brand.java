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
 * Entity đại diện cho bảng {@code brands}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "brands")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Brand {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu tên hiển thị của bản ghi. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Lưu mã nghiệp vụ của bản ghi. */
    @Column(name = "code", unique = true, length = 100)
    private String code;

    /** Lưu giá trị logo của bản ghi. */
    @Column(name = "logo", columnDefinition = "TEXT")
    private String logo;

    /** Lưu mô tả chi tiết của bản ghi. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Lưu trạng thái xử lý của bản ghi. */
    @Column(name = "status", length = 50)
    private String status;

    /** Lưu giá trị terminated at của bản ghi. */
    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

