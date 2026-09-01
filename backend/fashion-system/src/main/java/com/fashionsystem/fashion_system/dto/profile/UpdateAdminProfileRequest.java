package com.fashionsystem.fashion_system.dto.profile;

import com.fashionsystem.fashion_system.entity.Gender;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Những trường cá nhân nhân viên được phép tự cập nhật. */
public record UpdateAdminProfileRequest(
        @Size(max = 20) String phone,
        LocalDate dateOfBirth,
        Gender gender,
        @Size(max = 2000) String avatar) {
}
