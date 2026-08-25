package com.fashionsystem.fashion_system.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import com.fashionsystem.fashion_system.entity.SocialProvider;

/** Credential do Google Identity Services hoặc Facebook Login trả về frontend. */
public record SocialLoginRequest(
        @NotNull SocialProvider provider,
        @NotBlank @Size(max = 10000) String token) {
}
