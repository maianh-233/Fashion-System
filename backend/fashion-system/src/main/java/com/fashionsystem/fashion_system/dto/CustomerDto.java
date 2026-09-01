package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @NotBlank @Size(min = 3, max = 50)
    private String username;
    @Email @Size(max = 255)
    private String email;
    @Size(max = 20)
    private String phone;
    @Size(max = 255)
    private String fullName;
    private LocalDate dateOfBirth;
    @Pattern(regexp = "MALE|FEMALE|OTHER")
    private String gender;
    private String avatar;
    private Boolean active;
    private Boolean locked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
