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
 * Entity đại diện cho bảng {@code customer_profiles}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "customer_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfile {
    /** Lưu mã người dùng của bản ghi. */
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Lưu giá trị full name của bản ghi. */
    @Column(name = "full_name", length = 255)
    private String fullName;

    /** Lưu giá trị date of birth của bản ghi. */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Lưu giá trị gender của bản ghi. */
    @Column(name = "gender", length = 20)
    private String gender;

    /** Lưu giá trị avatar của bản ghi. */
    @Column(name = "avatar", columnDefinition = "TEXT")
    private String avatar;

    /** Lưu thời điểm tạo của bản ghi. */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Lưu thời điểm cập nhật của bản ghi. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}

