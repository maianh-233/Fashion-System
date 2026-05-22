package com.fashion.inventoryservice.dto.request.store;

import java.math.BigDecimal;

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
public class UpdateStoreRequest {

    private String name;

    private String address;

    private String phone;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean active;
}
