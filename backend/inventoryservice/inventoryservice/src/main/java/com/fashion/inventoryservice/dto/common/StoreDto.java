package com.fashion.inventoryservice.dto.common;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDto {

    private UUID id;

    private String code;

    private String name;

    private String address;

    private String phone;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean active;
}
