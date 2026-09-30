package com.fashionsystem.fashion_system.dto;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GoodsReceiptRequest {
    @NotNull private UUID storeId;
    @NotNull private UUID supplierId;
    private String note;
}
