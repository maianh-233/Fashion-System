package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.entity.SocialProvider;

/** Hồ sơ tối thiểu đã được backend xác minh với nhà cung cấp. */
public record SocialProfile(
        SocialProvider provider, String providerUserId, String email, String fullName, String avatar) {
}
