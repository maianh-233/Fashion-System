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
public class SupplierDto {

    private UUID id;

    private String code;

    private String name;

    private String contactName;

    private String phone;

    private String email;

    private String address;

    private String status;
}