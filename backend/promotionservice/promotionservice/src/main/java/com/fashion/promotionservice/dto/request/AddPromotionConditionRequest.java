package com.fashion.promotionservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddPromotionConditionRequest {

    @NotBlank
    private String conditionType;

    @NotBlank
    private String conditionValue;
}