package com.fashionsystem.fashion_system.dto;

import com.fashionsystem.fashion_system.entity.SocialProvider;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dữ liệu liên kết social an toàn, không công khai định danh provider của người dùng. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSocialAccountDto {
    private UUID id;
    private UUID customerId;
    private SocialProvider provider;
    private String providerEmail;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
