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
 * Entity đại diện cho bảng {@code user_tiers}, dùng để ánh xạ và thao tác dữ liệu của bảng này qua JPA.
 */
@Entity
@Table(name = "user_tiers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTier {
    /** Lưu mã định danh duy nhất của bản ghi. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    /** Lưu mã người dùng của bản ghi. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Lưu mã hạng khách hàng của bản ghi. */
    @Column(name = "tier_id", nullable = false)
    private UUID tierId;

    /** Lưu thời điểm gán của bản ghi. */
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    /** Lưu thời điểm hết hạn của bản ghi. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** Lưu ghi chú của bản ghi. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

}

