package com.fashion.inventoryservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class StoreResponse {

    private UUID id;

    private String code;

    private String name;

    private String address;

    private String phone;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean active;

    private LocalDateTime createdAt;
}