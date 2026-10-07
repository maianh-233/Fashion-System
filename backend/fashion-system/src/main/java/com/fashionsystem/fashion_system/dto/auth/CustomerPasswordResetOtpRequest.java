package com.fashionsystem.fashion_system.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerPasswordResetOtpRequest(
        @NotBlank @Email @Size(max = 255) String email) {
}

