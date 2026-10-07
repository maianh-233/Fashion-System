package com.fashionsystem.fashion_system.dto.customer;

import com.fashionsystem.fashion_system.entity.CustomerSource;
import java.util.UUID;

public record CustomerLookupResponse(
        UUID id, String customerCode, String fullName, String phone, String email,
        CustomerSource source, UUID originStoreId, boolean editable) {
}

