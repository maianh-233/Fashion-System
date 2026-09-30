package com.fashionsystem.fashion_system.dto;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GoodsReceiptItemRequest {
    @NotNull private UUID productId;
    @NotNull private UUID productVariantId;
    @NotNull @Min(1) private Integer quantity;
    @NotBlank @Pattern(regexp = "ONLINE|OFFLINE") private String targetChannel;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) private BigDecimal costPrice;
}
