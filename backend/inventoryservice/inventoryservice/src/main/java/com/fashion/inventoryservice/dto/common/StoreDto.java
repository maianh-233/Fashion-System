package com.fashion.inventoryservice.dto.common;

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

    private Boolean active;
}
