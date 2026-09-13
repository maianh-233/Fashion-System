package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PositionDto {
    private UUID id;
    @NotNull
    private UUID departmentId;
    private String departmentCode;
    private String departmentName;
    @NotBlank @Size(max = 50)
    private String code;
    @NotBlank @Size(max = 150)
    private String name;
    private String description;
    private Integer hierarchyLevel;
    private Long minSalary;
    private Long maxSalary;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
