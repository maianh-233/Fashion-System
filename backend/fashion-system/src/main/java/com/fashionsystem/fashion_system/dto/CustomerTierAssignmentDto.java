package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CustomerTierAssignmentDto {
    private UUID id;
    private UUID customerId;
    @NotNull
    private UUID tierId;
    private LocalDateTime assignedAt;
    private LocalDateTime expiresAt;
    private String note;
}
