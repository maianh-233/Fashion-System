package com.fashionsystem.fashion_system.dto.position;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreatePositionRequest(
        @NotNull UUID departmentId,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        String description,
        Boolean active,
        @NotNull @Positive Integer hierarchyLevel,
        @NotNull @PositiveOrZero Long minSalary,
        @NotNull @PositiveOrZero Long maxSalary) {}
