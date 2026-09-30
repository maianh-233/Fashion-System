package com.fashionsystem.fashion_system.dto;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GoodsIssueRequest {
    @NotNull private UUID storeId;
    private UUID supplierId;
    private String note;
    @NotBlank private String issueType;
    private String reason;
}
