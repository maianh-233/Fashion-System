package com.fashionsystem.fashion_system.mapper;

import com.fashionsystem.fashion_system.dto.CustomerSocialAccountDto;
import com.fashionsystem.fashion_system.entity.CustomerSocialAccount;
import com.fashionsystem.fashion_system.entity.SocialProvider;
import org.springframework.stereotype.Component;

/** Chuyển liên kết social sang DTO không lộ providerUserId. */
@Component
public class CustomerSocialAccountMapper {
    public CustomerSocialAccountDto toDto(CustomerSocialAccount entity) {
        if (entity == null) return null;
        return CustomerSocialAccountDto.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .provider(SocialProvider.valueOf(entity.getProvider()))
                .providerEmail(entity.getProviderEmail())
                .createdAt(entity.getCreatedAt())
                .lastLoginAt(entity.getLastLoginAt())
                .build();
    }
}
