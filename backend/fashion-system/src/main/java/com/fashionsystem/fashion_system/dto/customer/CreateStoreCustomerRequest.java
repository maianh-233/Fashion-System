package com.fashionsystem.fashion_system.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateStoreCustomerRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotBlank @Size(max = 20) String phone,
        @Email @Size(max = 255) String email,
        LocalDate birthday,
        @Pattern(regexp = "MALE|FEMALE|OTHER") String gender,
        @Size(max = 2000) String note,
        UUID originStoreId) {
}

