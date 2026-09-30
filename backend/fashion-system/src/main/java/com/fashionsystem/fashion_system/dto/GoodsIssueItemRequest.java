package com.fashionsystem.fashion_system.dto;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GoodsIssueItemRequest {
    @NotNull private UUID productId;
    @NotNull private UUID productVariantId;
    @NotNull @Min(1) private Integer quantity;
    @NotBlank @Pattern(regexp = "ONLINE|OFFLINE") private String sourceChannel;
}
