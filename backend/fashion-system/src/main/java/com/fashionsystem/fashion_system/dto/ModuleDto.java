package com.fashionsystem.fashion_system.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import jakarta.validation.constraints.*;

/** DTO metadata module, không expose entity JPA. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDto {
    private UUID id;
    @NotBlank @Size(max = 50) private String code;
    @NotBlank @Size(max = 100) private String name;
    private String description;
    @Size(max = 100) private String icon;
    @Min(0) private Integer sortOrder;
    private Boolean active;
    private LocalDateTime createdAt;
}
