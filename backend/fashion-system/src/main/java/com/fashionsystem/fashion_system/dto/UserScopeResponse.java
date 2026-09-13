package com.fashionsystem.fashion_system.dto;

import java.util.UUID;

/** Safe frontend projection of the authenticated employee's identity scope. */
public record UserScopeResponse(String scope, UUID storeId, String storeCode, String storeName) {
}
