package com.fashionsystem.fashion_system.dto.customer;

import com.fashionsystem.fashion_system.entity.CustomerMembershipStatus;
import com.fashionsystem.fashion_system.entity.CustomerSource;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerListResponse(
        UUID id,
        String customerCode,
        String fullName,
        String phone,
        String email,
        CustomerSource source,
        CustomerMembershipStatus membershipStatus,
        UUID originStoreId,
        String originStoreName,
        String tier,
        boolean hasWebAccount,
        Boolean active,
        LocalDateTime createdAt) {
}

