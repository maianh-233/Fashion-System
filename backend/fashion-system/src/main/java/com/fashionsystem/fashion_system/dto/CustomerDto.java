package com.fashionsystem.fashion_system.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dữ liệu khách hàng an toàn, không chứa mật khẩu băm. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerDto {
    private UUID id;
    private String username;
    private String email;
    private String phone;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String avatar;
    private Boolean active;
    private Boolean locked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
