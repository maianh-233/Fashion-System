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

/** Khách hàng bên ngoài, có định danh và thông tin đăng nhập riêng, không liên kết {@code users}. */
@Entity
@Table(name = "customers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "customer_code", nullable = false, unique = true, length = 30)
    private String customerCode;
    @Column(name = "email", unique = true, length = 255)
    private String email;
    @Column(name = "phone", unique = true, length = 20)
    private String phone;
    @Column(name = "normalized_phone", length = 20)
    private String normalizedPhone;
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private CustomerSource source;
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false, length = 20)
    private CustomerMembershipStatus membershipStatus;
    @Column(name = "origin_store_id")
    private UUID originStoreId;
    @Column(name = "active")
    private Boolean active;
    @Column(name = "full_name", length = 255)
    private String fullName;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name = "gender", length = 20)
    private String gender;
    @Column(name = "avatar", columnDefinition = "TEXT")
    private String avatar;
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
